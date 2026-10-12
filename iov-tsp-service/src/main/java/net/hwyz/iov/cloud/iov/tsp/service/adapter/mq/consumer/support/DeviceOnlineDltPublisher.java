package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspDeviceOnlineProperties;
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
 * DEVICE_ONLINE 失败隔离 DLT 发布器（TSP-DSN-CR-010 R1）
 * <p>
 * 永久异常或重试耗尽的记录写入 {@code tsp.device-online.dlt}：
 * <ul>
 *   <li>value 原样保留来源原始 JSON（受控敏感数据，靠 ACL/TLS/受限消费/有限保留期保护）；</li>
 *   <li>Kafka Key 使用规范化 hsmUid（无合法 hsmUid 时回退原始 Key）以保留同设备可追踪性；</li>
 *   <li>Headers 仅记录非敏感诊断信息；日志/指标/异常正文不得输出原始 payload 或完整身份。</li>
 * </ul>
 * 只有 DLT Broker 确认成功才由调用方提交源 offset。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceOnlineDltPublisher {

    private final ReactiveKafkaProducerTemplate<String, String> stringReactiveKafkaProducerTemplate;
    private final TspDeviceOnlineProperties properties;

    /**
     * 发布失败记录至 DLT。
     *
     * @param record    源记录
     * @param dltKey    规范化 hsmUid（可为 null，回退源 Key）
     * @param errorCode 稳定错误码
     * @param retryCount 已重试次数
     * @param digest    契约摘要（可为 null）
     * @param eventId   事件ID（可为 null）
     * @return Broker 确认后完成的 Mono；失败时以错误结束（调用方不得提交源 offset）
     */
    public Mono<Void> publish(ReceiverRecord<String, String> record,
                              String dltKey,
                              String errorCode,
                              int retryCount,
                              String digest,
                              String eventId) {
        TspDeviceOnlineProperties.Dlt dlt = properties.getDlt();
        if (!dlt.isEnabled()) {
            log.error("DLT 未启用，无法隔离失败记录, errorCode={}, sourceTopic={}", errorCode, record.topic());
            return Mono.error(new DeviceOnlineDltException("DLT_DISABLED", errorCode));
        }
        String key = dltKey != null ? dltKey : record.key();
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
                new ProducerRecord<>(dlt.getTopic(), null, key, record.value(), headers);
        return stringReactiveKafkaProducerTemplate.send(producerRecord)
                .doOnNext(result -> log.warn("DEVICE_ONLINE 已隔离至 DLT, errorCode={}, sourceTopic={}, sourcePartition={}, sourceOffset={}, retryCount={}",
                        errorCode, record.topic(), record.partition(), record.offset(), retryCount))
                .doOnError(e -> log.error("DEVICE_ONLINE 隔离至 DLT 失败, errorCode={}, sourceTopic={}, sourceOffset={}",
                        errorCode, record.topic(), record.offset(), e))
                .then();
    }

    private static void put(Headers headers, String key, String value) {
        if (value != null) {
            headers.add(key, value.getBytes(StandardCharsets.UTF_8));
        }
    }
}
