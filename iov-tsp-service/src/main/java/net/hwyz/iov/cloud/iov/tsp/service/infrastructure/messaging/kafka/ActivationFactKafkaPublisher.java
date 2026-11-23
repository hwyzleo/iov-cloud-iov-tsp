package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.messaging.kafka;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationFact;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.ActivationFactPublisher;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 首次激活候选事实 Kafka 生产适配（IOV-TSP-DSN-CR-017 D017-07）。
 * <p>
 * 载荷只包含最小必要字段，不发送账号 PII；Key=VIN 保证同车有序；
 * 投递失败由 Outbox 重试，不产生第二业务事实（VMD 按 eventId 幂等）。
 * <p>
 * 终审修复 B-4（R-10 定稿口径）：事件 payload 新增 {@code payloadDigest}
 * （{@code sha256(vin|bindingOperationId|sourceFactRef|occurredAt)}，与 eventId 无关），
 * 供 VMD 回算对拉与数据质量观测；<b>仅新事件生效，不参与幂等判定</b>（幂等键仍为 eventId）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivationFactKafkaPublisher implements ActivationFactPublisher {

    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(5);

    private final ReactiveKafkaProducerTemplate<String, String> stringReactiveKafkaProducerTemplate;
    private final TspUserVehicleProperties properties;

    @Override
    public void publish(ActivationFact fact) {
        if (!properties.getActivationFact().isPublishEnabled()) {
            throw new IllegalStateException("首次激活候选事实投递未启用");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", fact.getEventId());
        payload.put("schemaVersion", fact.getSchemaVersion());
        payload.put("vin", fact.getVin());
        payload.put("bindingOperationId", fact.getBindingOperationId());
        payload.put("sourceFactRef", fact.getSourceFactRef());
        payload.put("occurredAt", fact.getOccurredAt() == null ? null : fact.getOccurredAt().toString());
        // R-10：载荷摘要随事件投递（与 eventId 无关；occurredAt 取同一 Instant.toString()，便于 VMD 复算对拉）
        payload.put("payloadDigest", fact.getPayloadDigest());
        String json = JSONUtil.toJsonStr(payload);
        stringReactiveKafkaProducerTemplate
                .send(properties.getActivationFact().getTopic(), fact.getVin(), json)
                .block(SEND_TIMEOUT);
        log.info("投递首次激活候选事实: eventId={}, topic={}", fact.getEventId(),
                properties.getActivationFact().getTopic());
    }
}
