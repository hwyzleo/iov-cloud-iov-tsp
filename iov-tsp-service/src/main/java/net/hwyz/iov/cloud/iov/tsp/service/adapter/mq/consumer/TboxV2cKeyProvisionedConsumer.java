package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.event.TboxV2cKeyProvisionedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.TboxV2cKeyProvisionedConsumerMetrics;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.TboxV2cKeyProvisionedConsumerState;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.TboxV2cKeyProvisionedContractException;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.TboxV2cKeyProvisionedDltPublisher;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.TboxV2cActivationOutcome;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.TboxRegistrationAppService;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.support.TboxEventDigest;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspTboxV2cKeyProvisionedProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.retry.Retry;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * VMD V2C 聚合成功事件消费者（TSP-DSN-CR-012）
 * <p>
 * 订阅 {@code vmd.tbox-v2c-key-provisioned}（Kafka Key=deviceSn），独立 Consumer Group，手动 ack：
 * <ul>
 *   <li>契约/状态永久拒绝：直接写入 DLT，不自动重试；</li>
 *   <li>可恢复（跨 Topic 乱序/投影延迟/基础设施瞬时）异常：进程内指数退避最多 N 次，
 *       重试沿用原事件与幂等身份，不生成新 requestId；</li>
 *   <li>重试耗尽：写入 DLT 人工对账；</li>
 *   <li>仅 DLT Broker 确认（或成功/幂等终态）后才提交源 offset；DLT 发布失败不提交 offset。</li>
 * </ul>
 * 本消费者不调用 VMD Keyprov API、不接收 Request/Response/ACK/Status，
 * 不持久化 keyId/version（RD-012-1/RD-012-4）。发布门禁默认关闭。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TboxV2cKeyProvisionedConsumer {

    private final KafkaProperties kafkaProperties;
    private final TspTboxV2cKeyProvisionedProperties properties;
    private final TboxV2cKeyProvisionedEventTranslator translator;
    private final TboxRegistrationAppService tboxRegistrationAppService;
    private final TboxV2cKeyProvisionedDltPublisher dltPublisher;
    private final TboxV2cKeyProvisionedConsumerMetrics metrics;
    private final TboxV2cKeyProvisionedConsumerState state;

    private Disposable subscription;

    @PostConstruct
    public void start() {
        if (!properties.isEnabled()) {
            log.warn("V2C 聚合成功事件消费者已禁用, topic={}, groupId={}", properties.getTopic(), properties.getGroupId());
            return;
        }
        state.setStarted(true);
        log.info("开始监听 VMD V2C 聚合成功事件, topic={}, groupId={}", properties.getTopic(), properties.getGroupId());
        this.subscription = new ReactiveKafkaConsumerTemplate<>(buildReceiverOptions())
                .receive()
                .concatMap(this::handleRecord)
                .doOnSubscribe(s -> state.setSubscribed(true))
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(5))
                        .maxBackoff(Duration.ofMinutes(1))
                        .doBeforeRetry(signal -> log.warn("V2C 消费流中断，准备重连: {}",
                                signal.failure() == null ? "unknown" : signal.failure().getClass().getSimpleName())))
                .subscribe();
    }

    @PreDestroy
    public void stop() {
        if (subscription != null) {
            subscription.dispose();
        }
    }

    /**
     * 构建接收配置：显式覆盖独立 group.id，键值均使用 String 反序列化。
     */
    Map<String, Object> buildConsumerProperties() {
        Map<String, Object> consumerProps = new HashMap<>(kafkaProperties.buildConsumerProperties());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getGroupId());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return consumerProps;
    }

    /**
     * 构建 ReceiverOptions：仅订阅配置的源 Topic。
     */
    ReceiverOptions<String, String> buildReceiverOptions() {
        return ReceiverOptions.<String, String>create(buildConsumerProperties())
                .subscription(Collections.singleton(properties.getTopic()));
    }

    /**
     * 处理单条记录：成功/幂等直接 ack；失败按永久/可恢复策略隔离。
     */
    Mono<Void> handleRecord(ReceiverRecord<String, String> record) {
        state.setLastPollAt(Instant.now());
        return resolve(record, 1)
                .flatMap(decision -> {
                    if (decision.acked()) {
                        return Mono.empty();
                    }
                    metrics.dlt(decision.reason());
                    return dltPublisher.publish(record, decision.reason(), decision.retryCount(),
                            decision.digest(), decision.eventId());
                })
                .then(Mono.fromRunnable(() -> record.receiverOffset().acknowledge()));
    }

    private Mono<Decision> resolve(ReceiverRecord<String, String> record, int attempt) {
        Instant receivedAt = Instant.now();
        return Mono.fromCallable(() -> processOnce(record, receivedAt))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(processed -> {
                    TboxV2cActivationOutcome outcome = processed.outcome();
                    if (outcome.isSuccess()) {
                        metrics.processed(outcome);
                        if (outcome == TboxV2cActivationOutcome.ACTIVATED) {
                            metrics.activationLatency(processed.occurredAt());
                        }
                        return Mono.just(Decision.success());
                    }
                    if (outcome.isRetryable()) {
                        if (attempt >= properties.getRetry().getMaxAttempts()) {
                            log.error("V2C 事件处理重试耗尽, reason={}, sourceOffset={}, retryCount={}",
                                    outcome.name(), record.offset(), attempt);
                            return Mono.just(Decision.dlt("RETRY_EXHAUSTED_" + outcome.name(), attempt,
                                    processed.digest(), processed.eventId()));
                        }
                        metrics.retry(outcome.name());
                        return Mono.delay(backoff(attempt)).then(resolve(record, attempt + 1));
                    }
                    metrics.processed(outcome);
                    return Mono.just(Decision.dlt(outcome.name(), attempt,
                            processed.digest(), processed.eventId()));
                })
                .onErrorResume(TboxV2cKeyProvisionedContractException.class, e ->
                        Mono.just(Decision.dlt(e.getErrorCode(), attempt, null, null)))
                .onErrorResume(Throwable.class, e -> {
                    if (attempt >= properties.getRetry().getMaxAttempts()) {
                        log.error("V2C 事件处理重试耗尽, sourceOffset={}, errorClass={}",
                                record.offset(), e.getClass().getSimpleName());
                        return Mono.just(Decision.dlt("RETRY_EXHAUSTED", attempt, null, null));
                    }
                    log.warn("V2C 事件处理异常，准备重试, attempt={}, sourceOffset={}, errorClass={}",
                            attempt, record.offset(), e.getClass().getSimpleName());
                    metrics.retry("INFRA_ERROR");
                    return Mono.delay(backoff(attempt)).then(resolve(record, attempt + 1));
                });
    }

    private Processed processOnce(ReceiverRecord<String, String> record, Instant receivedAt) {
        TboxV2cKeyProvisionedEvent event =
                translator.translate(record.topic(), record.key(), record.value(), receivedAt);
        TboxV2cActivationOutcome outcome = tboxRegistrationAppService.handleV2cProvisioned(event);
        return new Processed(outcome, event.getEventId(), TboxEventDigest.v2c(event), event.getOccurredAt());
    }

    private Duration backoff(int attempt) {
        long initialMs = properties.getRetry().getInitialBackoff().toMillis();
        long factor = 1L << Math.min(attempt - 1, 20);
        long millis = initialMs > Long.MAX_VALUE / factor ? Long.MAX_VALUE : initialMs * factor;
        long capped = Math.min(millis, properties.getRetry().getMaxBackoff().toMillis());
        return Duration.ofMillis(Math.max(capped, 0));
    }

    private record Processed(TboxV2cActivationOutcome outcome, String eventId,
                             String digest, Instant occurredAt) {
    }

    private record Decision(boolean acked, String reason, int retryCount, String digest, String eventId) {

        static Decision success() {
            return new Decision(true, null, 0, null, null);
        }

        static Decision dlt(String reason, int retryCount, String digest, String eventId) {
            return new Decision(false, reason, retryCount, digest, eventId);
        }
    }
}
