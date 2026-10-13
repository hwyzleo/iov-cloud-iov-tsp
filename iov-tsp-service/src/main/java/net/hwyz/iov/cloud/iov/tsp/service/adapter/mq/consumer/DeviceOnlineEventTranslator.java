package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.api.vo.event.VagwDeviceOnlineEvent;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.DeviceOnlineContractException;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.command.RecordTboxOnlineCmd;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.DeviceUidNormalizer;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.SensitiveDataMasker;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * DEVICE_ONLINE 事件防腐转换与契约校验（TSP-DSN-CR-010 §3）
 * <p>
 * 将 {@code vagw.device.online} 原始 JSON 转换为内部 {@link RecordTboxOnlineCmd}；
 * 校验 Kafka Key、eventType、hsmUid、sessionId、connectionGeneration、occurredAt 与 timeSource。
 * 校验失败抛出 {@link DeviceOnlineContractException}（永久异常，进入 DLT）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceOnlineEventTranslator {

    private static final int MAX_EVENT_ID = 64;
    private static final int MAX_SESSION_ID = 128;
    private static final int MAX_TBOX_SN = 64;
    private static final int MAX_CERT_SERIAL = 128;

    private static final Duration MAX_FUTURE_SKEW = Duration.ofMinutes(5);
    private static final Duration MAX_PAST_WINDOW = Duration.ofDays(30);

    private static final Set<String> TIME_SOURCE_ALLOWLIST =
            Set.of(VagwDeviceOnlineEvent.TIME_SOURCE_EMQX, VagwDeviceOnlineEvent.TIME_SOURCE_VAGW_RECEIVED);

    private final ObjectMapper objectMapper;

    /**
     * 校验并转换为内部命令。
     *
     * @param kafkaKey Kafka Key（应等于规范化 hsmUid）
     * @param payload  原始 JSON
     * @param receivedAt 接收时间（时钟边界基准）
     * @param sourceTopic 来源 Topic
     * @return 内部上线命令（hsmUid 已规范化）
     */
    public RecordTboxOnlineCmd translate(String kafkaKey, String payload, Instant receivedAt, String sourceTopic) {
        if (StrUtil.isBlank(payload)) {
            throw new DeviceOnlineContractException("EMPTY_PAYLOAD", "DEVICE_ONLINE payload 为空");
        }
        VagwDeviceOnlineEvent event;
        try {
            event = objectMapper.readValue(payload, VagwDeviceOnlineEvent.class);
        } catch (Exception e) {
            throw new DeviceOnlineContractException("MALFORMED_JSON", "DEVICE_ONLINE payload 无法解析");
        }
        if (event == null) {
            throw new DeviceOnlineContractException("MALFORMED_JSON", "DEVICE_ONLINE payload 解析为空");
        }

        String hsmUid = DeviceUidNormalizer.normalize(event.getHsmUid());
        if (!DeviceUidNormalizer.isValid(hsmUid)) {
            throw new DeviceOnlineContractException("INVALID_HSM_UID", "hsmUid 非法或超长");
        }
        if (StrUtil.isBlank(kafkaKey) || !kafkaKey.equals(hsmUid)) {
            throw new DeviceOnlineContractException("KEY_HSM_UID_MISMATCH",
                    "Kafka Key 为空或与 hsmUid 不一致, key摘要=" + SensitiveDataMasker.maskHsmUid(kafkaKey));
        }
        if (!VagwDeviceOnlineEvent.TYPE.equals(event.getEventType())) {
            throw new DeviceOnlineContractException("INVALID_EVENT_TYPE", "eventType 非 DEVICE_ONLINE");
        }
        if (StrUtil.isBlank(event.getEventId()) || event.getEventId().length() > MAX_EVENT_ID) {
            throw new DeviceOnlineContractException("INVALID_EVENT_ID", "eventId 缺失或超长");
        }
        if (StrUtil.isBlank(event.getSessionId()) || event.getSessionId().length() > MAX_SESSION_ID) {
            throw new DeviceOnlineContractException("INVALID_SESSION_ID", "sessionId 缺失或超长");
        }
        if (event.getConnectionGeneration() <= 0) {
            throw new DeviceOnlineContractException("INVALID_GENERATION", "connectionGeneration 必须大于 0");
        }
        if (event.getOccurredAt() == null) {
            throw new DeviceOnlineContractException("MISSING_OCCURRED_AT", "occurredAt 缺失");
        }
        Instant occurredAt = event.getOccurredAt();
        if (occurredAt.isAfter(receivedAt.plus(MAX_FUTURE_SKEW))) {
            throw new DeviceOnlineContractException("OCCURRED_AT_FUTURE", "occurredAt 超前于允许时钟边界");
        }
        if (occurredAt.isBefore(receivedAt.minus(MAX_PAST_WINDOW))) {
            throw new DeviceOnlineContractException("OCCURRED_AT_TOO_OLD", "occurredAt 早于允许边界");
        }
        if (StrUtil.isBlank(event.getTimeSource()) || !TIME_SOURCE_ALLOWLIST.contains(event.getTimeSource())) {
            throw new DeviceOnlineContractException("INVALID_TIME_SOURCE", "timeSource 非法");
        }
        if (event.getTboxSn() != null && event.getTboxSn().length() > MAX_TBOX_SN) {
            throw new DeviceOnlineContractException("INVALID_TBOX_SN", "tboxSn 超长");
        }
        if (event.getCertSerial() != null && event.getCertSerial().length() > MAX_CERT_SERIAL) {
            throw new DeviceOnlineContractException("INVALID_CERT_SERIAL", "certSerial 超长");
        }

        return new RecordTboxOnlineCmd(
                event.getEventId(),
                hsmUid,
                event.getVin(),
                event.getTboxSn(),
                event.getSessionId(),
                event.getConnectionGeneration(),
                occurredAt,
                event.getTimeSource(),
                sourceTopic);
    }
}
