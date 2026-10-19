package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspTboxV2cKeyProvisionedProperties;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverRecord;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * V2C 聚合成功事件失败隔离 DLT 发布器（TSP-DSN-CR-012 §8）
 * <p>
 * 永久契约/状态拒绝或重试耗尽的记录写入 {@code tsp.tbox-v2c-key-provisioned.dlt}：
 * <ul>
 *   <li>value 原样保留来源非敏感元数据 JSON（不新增 proof/证书/Wrapped Key/KMS 引用）；</li>
 *   <li>Kafka Key 沿用源 Key（deviceSn）以保留同设备可追踪性；</li>
 *   <li>Headers 仅记录非敏感诊断信息（errorCode/retryCount/contractDigest/eventId 等），
 *       不包含 keyId 列表或密码学材料；</li>
 *   <li>日志仅输出 errorCode 与来源位置，禁止输出原始 payload 与 keyId。</li>
 * </ul>
 * 只有 DLT Broker 确认成功才由调用方提交源 offset。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TboxV2cKeyProvisionedDltPublisher {

    private final ReactiveKafkaProducerTemplate<String, String> stringReactiveKafkaProducerTemplate;
    private final TspTboxV2cKeyProvisionedProperties properties;

    /**
     * 发布失败记录至 DLT。
     *
     * @param record     源记录
     * @param errorCode  稳定错误码
     * @param retryCount 已重试次数
     * @param digest     契约摘要（可为 null）
     * @param eventId    事件ID（可为 null）
     * @return Broker 确认后完成的 Mono；失败时以错误结束（调用方不得提交源 offset）
     */
    public Mono<Void> publish(ReceiverRecord<String, String> record,
                              String errorCode,
                              int retryCount,
                              String digest,
                              String eventId) {
        TspTboxV2cKeyProvisionedProperties.Dlt dlt = properties.getDlt();
        if (dlt == null || !dlt.isEnabled()) {
            log.error("V2C DLT 未启用，无法隔离失败记录, errorCode={}, sourceTopic={}", errorCode, record.topic());
            return Mono.error(new TboxV2cKeyProvisionedDltException("DLT_DISABLED", errorCode));
        }
        Headers headers = new RecordHeaders();
        put(headers, "sourceTopic", record.topic());
        put(headers, "sourcePartition", String.valueOf(record.partition()));
        put(headers, "sourceOffset", String.valueOf(record.offset()));
        put(headers, "consumerGroup", properties.getGroupId());
        put(headers, "errorCode", errorCode);
        put(headers, "failedAt", Instant.now().toString());
        put(headers, "retryCount", String.valueOf(retryCount));
        put(headers, "contractDigest", digest);
        put(headers, "eventId", eventId);
        ProducerRecord<String, String> producerRecord =
                new ProducerRecord<>(dlt.getTopic(), null, record.key(), record.value(), headers);
        return stringReactiveKafkaProducerTemplate.send(producerRecord)
                .doOnNext(result -> log.warn("V2C 事件已隔离至 DLT, errorCode={}, sourceTopic={}, sourcePartition={}, sourceOffset={}, retryCount={}",
                        errorCode, record.topic(), record.partition(), record.offset(), retryCount))
                .doOnError(e -> log.error("V2C 事件隔离至 DLT 失败, errorCode={}, sourceTopic={}, sourceOffset={}, errorClass={}",
                        errorCode, record.topic(), record.offset(), e.getClass().getSimpleName()))
                .then();
    }

    private static void put(Headers headers, String key, String value) {
        if (value != null) {
            headers.add(key, value.getBytes(StandardCharsets.UTF_8));
        }
    }
}
