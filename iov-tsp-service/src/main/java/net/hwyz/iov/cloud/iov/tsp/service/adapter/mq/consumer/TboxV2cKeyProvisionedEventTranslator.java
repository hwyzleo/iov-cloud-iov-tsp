package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.api.vo.event.TboxV2cKeyProvisionedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.TboxV2cKeyProvisionedContractException;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspTboxV2cKeyProvisionedProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * VMD V2C 聚合成功事件防腐转换与契约校验（TSP-DSN-CR-012 §4/§5）
 * <p>
 * 消费流水线：topic → key/value size → schema/契约版本 → 规范化 Kafka Key == deviceSn →
 * businessDomain → result → keys 用途完整性 → keyId/version → occurredAt。
 * 任何契约失败抛出 {@link TboxV2cKeyProvisionedContractException}（永久异常，进入 DLT，不自动重试）。
 * <p>
 * 安全边界：本转换器只输出非敏感元数据，keyId 不落库、不写日志、不进指标。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TboxV2cKeyProvisionedEventTranslator {

    private static final int MAX_EVENT_ID = 64;
    private static final int MAX_REQUEST_ID = 128;
    private static final int MAX_DEVICE_SN = 64;
    private static final int MAX_KEY_ID = 256;
    private static final int MAX_PURPOSE = 64;
    private static final int MAX_CONTRACT_VERSION = 16;

    private static final Set<String> REQUIRED_PURPOSES = Set.of(
            TboxV2cKeyProvisionedEvent.PURPOSE_UPLINK_AEAD,
            TboxV2cKeyProvisionedEvent.PURPOSE_DOWNLINK_AEAD);

    private final ObjectMapper objectMapper;
    private final TspTboxV2cKeyProvisionedProperties properties;

    /**
     * 校验并转换为内部事件对象。
     *
     * @param topic      Kafka Topic（应等于配置 Topic）
     * @param kafkaKey   Kafka Key（应等于规范化 deviceSn）
     * @param payload    原始 JSON
     * @param receivedAt 接收时间（时钟边界基准）
     * @return 经规范化与校验的事件（deviceSn 已 trim、contractVersion 已归一）
     */
    public TboxV2cKeyProvisionedEvent translate(String topic, String kafkaKey, String payload, Instant receivedAt) {
        if (StrUtil.isBlank(topic) || !topic.equals(properties.getTopic())) {
            throw new TboxV2cKeyProvisionedContractException("UNEXPECTED_TOPIC", "Topic 与订阅契约不一致");
        }
        if (StrUtil.isBlank(payload)) {
            throw new TboxV2cKeyProvisionedContractException("EMPTY_PAYLOAD", "V2C 事件 payload 为空");
        }
        if (payload.getBytes(StandardCharsets.UTF_8).length > properties.getMaxPayloadBytes()) {
            throw new TboxV2cKeyProvisionedContractException("PAYLOAD_TOO_LARGE", "V2C 事件 payload 超出允许大小");
        }
        if (StrUtil.isNotBlank(kafkaKey)
                && kafkaKey.getBytes(StandardCharsets.UTF_8).length > properties.getMaxDeviceSnBytes()) {
            throw new TboxV2cKeyProvisionedContractException("INVALID_DEVICE_SN", "Kafka Key 超长");
        }

        TboxV2cKeyProvisionedEvent event;
        try {
            event = objectMapper.readValue(payload, TboxV2cKeyProvisionedEvent.class);
        } catch (Exception e) {
            throw new TboxV2cKeyProvisionedContractException("MALFORMED_JSON", "V2C 事件 payload 无法解析");
        }
        if (event == null) {
            throw new TboxV2cKeyProvisionedContractException("MALFORMED_JSON", "V2C 事件 payload 解析为空");
        }

        validateContractVersion(event.getContractVersion());
        event.setContractVersion(normalizeContractVersion(event.getContractVersion()));

        String deviceSn = event.getDeviceSn() == null ? null : event.getDeviceSn().trim();
        if (StrUtil.isBlank(deviceSn) || deviceSn.length() > MAX_DEVICE_SN) {
            throw new TboxV2cKeyProvisionedContractException("INVALID_DEVICE_SN", "deviceSn 缺失或超长");
        }
        String normalizedKey = kafkaKey == null ? null : kafkaKey.trim();
        if (StrUtil.isBlank(normalizedKey) || !normalizedKey.equals(deviceSn)) {
            throw new TboxV2cKeyProvisionedContractException("KEY_DEVICE_SN_MISMATCH", "Kafka Key 为空或与 deviceSn 不一致");
        }
        event.setDeviceSn(deviceSn);

        if (StrUtil.isBlank(event.getEventId()) || event.getEventId().length() > MAX_EVENT_ID) {
            throw new TboxV2cKeyProvisionedContractException("INVALID_EVENT_ID", "eventId 缺失或超长");
        }
        if (StrUtil.isBlank(event.getRequestId()) || event.getRequestId().length() > MAX_REQUEST_ID) {
            throw new TboxV2cKeyProvisionedContractException("INVALID_REQUEST_ID", "requestId 缺失或超长");
        }
        if (!TboxV2cKeyProvisionedEvent.BUSINESS_DOMAIN_V2C_COMM.equals(event.getBusinessDomain())) {
            throw new TboxV2cKeyProvisionedContractException("INVALID_BUSINESS_DOMAIN", "businessDomain 非严格 V2C_COMM");
        }
        if (!TboxV2cKeyProvisionedEvent.RESULT_SUCCEEDED.equals(event.getResult())) {
            throw new TboxV2cKeyProvisionedContractException("RESULT_NOT_SUCCEEDED", "result 非严格 SUCCEEDED");
        }
        validateKeys(event.getKeys());
        validateOccurredAt(event.getOccurredAt(), receivedAt);
        return event;
    }

    private void validateContractVersion(String contractVersion) {
        if (StrUtil.isBlank(contractVersion)) {
            return;
        }
        if (contractVersion.length() > MAX_CONTRACT_VERSION
                || properties.getSupportedContractVersions() == null
                || !properties.getSupportedContractVersions().contains(contractVersion)) {
            throw new TboxV2cKeyProvisionedContractException("UNSUPPORTED_CONTRACT_VERSION",
                    "事件契约版本不受支持，fail-closed");
        }
    }

    private String normalizeContractVersion(String contractVersion) {
        if (StrUtil.isBlank(contractVersion)) {
            return TboxV2cKeyProvisionedEvent.CONTRACT_VERSION_V1;
        }
        return TboxV2cKeyProvisionedEvent.CONTRACT_VERSION_V1_ALIAS.equals(contractVersion)
                ? TboxV2cKeyProvisionedEvent.CONTRACT_VERSION_V1
                : contractVersion;
    }

    private void validateKeys(List<TboxV2cKeyProvisionedEvent.Key> keys) {
        if (keys == null || keys.size() != REQUIRED_PURPOSES.size()) {
            throw new TboxV2cKeyProvisionedContractException("INVALID_KEY_SET", "keys 必须恰好为双用途");
        }
        Set<String> purposes = new HashSet<>();
        for (TboxV2cKeyProvisionedEvent.Key key : keys) {
            if (key == null) {
                throw new TboxV2cKeyProvisionedContractException("INVALID_KEY_SET", "keys 含空元素");
            }
            if (StrUtil.isBlank(key.getPurpose()) || key.getPurpose().length() > MAX_PURPOSE
                    || !REQUIRED_PURPOSES.contains(key.getPurpose())) {
                throw new TboxV2cKeyProvisionedContractException("INVALID_KEY_SET", "keys 含未知用途");
            }
            if (!purposes.add(key.getPurpose())) {
                throw new TboxV2cKeyProvisionedContractException("DUPLICATE_PURPOSE", "keys 用途重复");
            }
            if (StrUtil.isBlank(key.getKeyId()) || key.getKeyId().length() > MAX_KEY_ID) {
                throw new TboxV2cKeyProvisionedContractException("INVALID_KEY_ID", "keyId 缺失或超长");
            }
            if (key.getBusinessKeyVersion() == null || key.getBusinessKeyVersion() <= 0) {
                throw new TboxV2cKeyProvisionedContractException("INVALID_KEY_VERSION", "businessKeyVersion 必须大于 0");
            }
        }
        if (!purposes.equals(REQUIRED_PURPOSES)) {
            throw new TboxV2cKeyProvisionedContractException("INVALID_KEY_SET", "keys 未覆盖完整用途集");
        }
    }

    private void validateOccurredAt(Instant occurredAt, Instant receivedAt) {
        if (occurredAt == null) {
            throw new TboxV2cKeyProvisionedContractException("MISSING_OCCURRED_AT", "occurredAt 缺失");
        }
        if (receivedAt != null && occurredAt.isAfter(receivedAt.plus(properties.getMaxFutureSkew()))) {
            throw new TboxV2cKeyProvisionedContractException("OCCURRED_AT_FUTURE", "occurredAt 超前于允许时钟边界");
        }
    }
}
