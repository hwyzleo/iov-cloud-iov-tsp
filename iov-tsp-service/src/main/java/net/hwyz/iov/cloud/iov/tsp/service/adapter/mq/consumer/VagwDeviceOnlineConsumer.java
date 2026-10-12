package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.DeviceOnlineConsumerMetrics;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.DeviceOnlineConsumerState;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.DeviceOnlineContractException;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.DeviceOnlineDltPublisher;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.command.RecordTboxOnlineCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.DeviceOnlineOutcome;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.TboxRegistrationAppService;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.support.TboxEventDigest;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspDeviceOnlineProperties;
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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * VAGW 设备上线事件消费者（TSP-DSN-CR-010）
 * <p>
 * 订阅 {@code vagw.device.online}（Kafka Key=hsmUid），独立 Consumer Group，手动 ack：
 * <ul>
 *   <li>可恢复（瞬时/对账）异常：进程内指数退避最多 N 次；</li>
 *   <li>永久异常或重试耗尽：写入 {@code tsp.device-online.dlt}，仅 DLT Broker 确认后提交源 offset；</li>
 *   <li>DLT 发布失败：不提交源 offset，交由 Kafka 重投/恢复；</li>
 *   <li>不同源 Consumer 共享 group.id 之外，本消费者显式覆盖为独立 group.id。</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VagwDeviceOnlineConsumer {

    private static final Set<DeviceOnlineOutcome> RETRYABLE = Set.of(
            DeviceOnlineOutcome.RETRY_TBOX_NOT_FOUND,
            DeviceOnlineOutcome.RETRY_UNBOUND);

    private final KafkaProperties kafkaProperties;
    private final TspDeviceOnlineProperties properties;
    private final DeviceOnlineEventTranslator translator;
    private final TboxRegistrationAppService tboxRegistrationAppService;
    private final DeviceOnlineDltPublisher dltPublisher;
    private final DeviceOnlineConsumerMetrics metrics;
    private final DeviceOnlineConsumerState state;

    private Disposable subscription;

    @PostConstruct
    public void start() {
        if (!properties.isEnabled()) {
            log.warn("DEVICE_ONLINE 消费者已禁用, topic={}", properties.getTopic());
            return;
        }
        state.setStarted(true);
        log.info("开始监听 VAGW 设备上线事件, topic={}, groupId={}", properties.getTopic(), properties.getGroupId());
        this.subscription = new ReactiveKafkaConsumerTemplate<>(buildReceiverOptions())
                .receive()
                .concatMap(this::handleRecord)
                .doOnSubscribe(s -> state.setSubscribed(true))
                .retryWhen(reactor.util.retry.Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(5))
                        .maxBackoff(Duration.ofMinutes(1))
                        .doBeforeRetry(signal -> log.warn("DEVICE_ONLINE 消费流中断，准备重连: {}",
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
     * 构建 ReceiverOptions：订阅源 Topic（Group 已在上方显式覆盖）。
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
                        return Mono.<Void>empty();
                    }
                    return dltPublisher.publish(record, decision.dltKey(), decision.errorCode(),
                                    decision.retryCount(), decision.digest(), decision.eventId())
                            .doOnSuccess(v -> metrics.rejected(decision.errorCode()));
                })
                .then(Mono.fromRunnable(() -> record.receiverOffset().acknowledge()));
    }

    private Mono<Decision> resolve(ReceiverRecord<String, String> record, int attempt) {
        Instant receivedAt = Instant.now();
        return Mono.fromCallable(() -> processOnce(record, receivedAt))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(processed -> {
                    DeviceOnlineOutcome outcome = processed.outcome();
                    if (outcome.isSuccess()) {
                        metrics.success(outcome);
                        return Mono.just(Decision.success());
                    }
                    if (RETRYABLE.contains(outcome)) {
                        if (attempt >= properties.getRetry().getMaxAttempts()) {
                            return Mono.just(Decision.dlt(processed,
                                    "RETRY_EXHAUSTED_" + outcome.name(), attempt));
                        }
                        metrics.retry();
                        return Mono.delay(backoff(attempt)).then(resolve(record, attempt + 1));
                    }
                    return Mono.just(Decision.dlt(processed, outcome.name(), attempt));
                })
                .onErrorResume(DeviceOnlineContractException.class, e ->
                        Mono.just(Decision.dlt(record.key(), e.getErrorCode(), attempt, null, null)))
                .onErrorResume(Throwable.class, e -> {
                    if (attempt >= properties.getRetry().getMaxAttempts()) {
                        log.error("DEVICE_ONLINE 处理重试耗尽, sourceOffset={}, errorClass={}",
                                record.offset(), e.getClass().getSimpleName());
                        return Mono.just(Decision.dlt(record.key(), "RETRY_EXHAUSTED", attempt, null, null));
                    }
                    log.warn("DEVICE_ONLINE 处理异常，准备重试, attempt={}, sourceOffset={}, errorClass={}",
                            attempt, record.offset(), e.getClass().getSimpleName());
                    metrics.retry();
                    return Mono.delay(backoff(attempt)).then(resolve(record, attempt + 1));
                });
    }

    private Processed processOnce(ReceiverRecord<String, String> record, Instant receivedAt) {
        RecordTboxOnlineCmd cmd = translator.translate(record.key(), record.value(), receivedAt, properties.getTopic());
        DeviceOnlineOutcome outcome = tboxRegistrationAppService.handleOnline(cmd);
        return new Processed(outcome, cmd.hsmUid(), TboxEventDigest.deviceOnline(cmd), cmd.eventId());
    }

    private Duration backoff(int attempt) {
        long initialMs = properties.getRetry().getInitialBackoff().toMillis();
        long factor = 1L << Math.min(attempt - 1, 20);
        long millis = initialMs > Long.MAX_VALUE / factor ? Long.MAX_VALUE : initialMs * factor;
        long capped = Math.min(millis, properties.getRetry().getMaxBackoff().toMillis());
        return Duration.ofMillis(Math.max(capped, 0));
    }

    private record Processed(DeviceOnlineOutcome outcome, String hsmUid, String digest, String eventId) {
    }

    private record Decision(boolean acked, String dltKey, String errorCode,
                            int retryCount, String digest, String eventId) {

        static Decision success() {
            return new Decision(true, null, null, 0, null, null);
        }

        static Decision dlt(Processed processed, String errorCode, int retryCount) {
            return new Decision(false, processed.hsmUid(), errorCode, retryCount, processed.digest(), processed.eventId());
        }

        static Decision dlt(String dltKey, String errorCode, int retryCount, String digest, String eventId) {
            return new Decision(false, dltKey, errorCode, retryCount, digest, eventId);
        }
    }
}
