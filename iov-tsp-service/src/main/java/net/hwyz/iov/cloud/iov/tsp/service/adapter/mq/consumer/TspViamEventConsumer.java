package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.ViamSecurityProjectionAppService;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TspViamInboxRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TspViamInboxRecord;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspViamProjectionProperties;
import net.hwyz.iov.cloud.sec.viam.api.event.AccessInvalidatedEvent;
import net.hwyz.iov.cloud.sec.viam.api.event.BusinessKeyChangedEvent;
import net.hwyz.iov.cloud.sec.viam.api.event.DeviceLoginSucceededEvent;
import net.hwyz.iov.cloud.sec.viam.api.event.DeviceOnboardingChangedEvent;
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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * VIAM 安全事件消费者（IOV-TSP-DSN-CR-016 / D016-04）
 * <p>
 * 订阅 VIAM 官方契约事件，独立 Consumer Group，手动 ack：
 * <ul>
 *   <li>Inbox 先落库（eventId 唯一键去重，仅契约摘要，<b>不存敏感 payload 原文</b>）；</li>
 *   <li>成功接入事件独立更新最近登录事实；状态变化事件仅触发 VIAM 权威快照刷新（不推断状态）；</li>
 *   <li>资产未导入 → Inbox RETRYABLE 待对账，不静默丢弃；</li>
 *   <li>瞬时异常进程内指数退避重试，耗尽标记 FAILED 并 ack（错误可见，可重放），不阻塞消费流。</li>
 * </ul>
 * 旧 TSP 安全消费者（DEVICE_ONLINE/V2C）停用后启用本消费者（联合切换条件）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TspViamEventConsumer {

    private final KafkaProperties kafkaProperties;
    private final TspViamProjectionProperties properties;
    private final ViamSecurityProjectionAppService projectionAppService;
    private final TspViamInboxRepository inboxRepository;
    private final ObjectMapper objectMapper;

    private Disposable subscription;

    @PostConstruct
    public void start() {
        if (!properties.isEnabled()) {
            log.warn("VIAM 安全投影消费者已禁用（旧 TSP 安全写链停用后启用）");
            return;
        }
        log.info("开始监听 VIAM 安全事件, topics={}, groupId={}",
                topics(), properties.getConsumer().getGroupId());
        this.subscription = new ReactiveKafkaConsumerTemplate<>(buildReceiverOptions())
                .receive()
                .concatMap(this::handleRecord)
                .retryWhen(reactor.util.retry.Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(5))
                        .maxBackoff(Duration.ofMinutes(1))
                        .doBeforeRetry(signal -> log.warn("VIAM 投影消费流中断，准备重连: {}",
                                signal.failure() == null ? "unknown" : signal.failure().getClass().getSimpleName())))
                .subscribe();
    }

    @PreDestroy
    public void stop() {
        if (subscription != null) {
            subscription.dispose();
        }
    }

    private Set<String> topics() {
        TspViamProjectionProperties.Topics topics = properties.getConsumer().getTopics();
        return Set.of(topics.getLoginSucceeded(), topics.getOnboardingChanged(),
                topics.getAccessInvalidated(), topics.getV2cKeyProvisioned());
    }

    Map<String, Object> buildConsumerProperties() {
        Map<String, Object> consumerProps = new HashMap<>(kafkaProperties.buildConsumerProperties());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getConsumer().getGroupId());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return consumerProps;
    }

    ReceiverOptions<String, String> buildReceiverOptions() {
        return ReceiverOptions.<String, String>create(buildConsumerProperties())
                .subscription(topics());
    }

    Mono<Void> handleRecord(ReceiverRecord<String, String> record) {
        return resolve(record, 1)
                .onErrorResume(Throwable.class, e -> {
                    // 重试耗尽：标记 FAILED 并 ack（错误可见、可重放；不阻塞消费流）
                    log.error("VIAM 投影事件处理失败, topic={}, eventKey={}, offset={}, errorClass={}",
                            record.topic(), mask(record.key()), record.offset(), e.getClass().getSimpleName(), e);
                    inboxRepository.markFailed(eventIdOf(record), safeSummary(e));
                    return Mono.empty();
                })
                .then(Mono.fromRunnable(() -> record.receiverOffset().acknowledge()));
    }

    private Mono<Void> resolve(ReceiverRecord<String, String> record, int attempt) {
        return Mono.defer(() -> {
                    try {
                        processOnce(record);
                        return Mono.empty();
                    } catch (ViamSecurityProjectionAppService.TboxNotFoundProjectionException e) {
                        return retryable(record, e);
                    } catch (Throwable e) {
                        return retryOrFail(record, attempt, e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 资产未导入/关联缺失：可重试/对账路径，不静默丢弃。
     */
    private Mono<Void> retryable(ReceiverRecord<String, String> record, Throwable e) {
        log.warn("VIAM 投影事件资产未导入，标记 RETRYABLE 待对账, topic={}, eventKey={}, err={}",
                record.topic(), mask(record.key()), e.getMessage());
        inboxRepository.markRetryable(eventIdOf(record), safeSummary(e));
        return Mono.empty();
    }

    /**
     * 瞬时异常：进程内指数退避重试，耗尽标记 FAILED 并 ack（错误可见、可重放）。
     */
    private Mono<Void> retryOrFail(ReceiverRecord<String, String> record, int attempt, Throwable e) {
        if (attempt >= properties.getConsumer().getRetry().getMaxAttempts()) {
            return Mono.error(e);
        }
        log.warn("VIAM 投影事件处理异常，准备重试, attempt={}, topic={}, eventKey={}, errorClass={}",
                attempt, record.topic(), mask(record.key()), e.getClass().getSimpleName());
        return Mono.delay(backoff(attempt)).then(resolve(record, attempt + 1));
    }

    /**
     * 单次处理：解析 → Inbox 去重 → 投影应用。
     */
    void processOnce(ReceiverRecord<String, String> record) throws Exception {
        String topic = record.topic();
        String payload = record.value();
        String eventId = eventIdOf(record);
        String digest = sha256(payload);

        // Inbox 去重：已 PROCESSED/CONFLICT 跳过；PENDING/RETRYABLE/FAILED 允许重新处理
        TspViamInboxRecord existing = inboxRepository.findByEventId(eventId).orElse(null);
        if (existing != null && (TspViamInboxRecord.STATUS_PROCESSED.equals(existing.status())
                || TspViamInboxRecord.STATUS_CONFLICT.equals(existing.status()))) {
            log.info("VIAM 投影事件已处理，幂等跳过 eventId={}", eventId);
            return;
        }
        if (existing != null && existing.contractDigest() != null && !existing.contractDigest().equals(digest)) {
            // 同 eventId 内容不同：契约冲突，不静默当重复成功
            log.error("VIAM 投影事件契约冲突 eventId={}（同一幂等键不同内容）", eventId);
            inboxRepository.markConflict(eventId, "CONTRACT_DIGEST_MISMATCH");
            return;
        }

        inboxRepository.save(new TspViamInboxRecord(eventId, topic, eventTypeOf(topic), record.key(),
                principalIdOf(topic, payload), hsmUidOf(topic, payload), deviceSnOf(topic, payload),
                digest, TspViamInboxRecord.STATUS_PENDING, 0, null,
                Instant.now(), null));

        dispatch(topic, payload);
        inboxRepository.markProcessed(eventId, LocalDateTime.now());
    }

    private void dispatch(String topic, String payload) throws Exception {
        TspViamProjectionProperties.Topics topics = properties.getConsumer().getTopics();
        if (topics.getLoginSucceeded().equals(topic)) {
            projectionAppService.handleLoginSucceeded(
                    objectMapper.readValue(payload, DeviceLoginSucceededEvent.class));
        } else if (topics.getOnboardingChanged().equals(topic)) {
            projectionAppService.handleOnboardingChanged(
                    objectMapper.readValue(payload, DeviceOnboardingChangedEvent.class));
        } else if (topics.getAccessInvalidated().equals(topic)) {
            projectionAppService.handleAccessInvalidated(
                    objectMapper.readValue(payload, AccessInvalidatedEvent.class));
        } else if (topics.getV2cKeyProvisioned().equals(topic)) {
            projectionAppService.handleV2cKeyProvisioned(
                    objectMapper.readValue(payload, BusinessKeyChangedEvent.class));
        } else {
            throw new IllegalArgumentException("未知 VIAM 事件 Topic: " + topic);
        }
    }

    private String eventIdOf(ReceiverRecord<String, String> record) {
        // 幂等键：优先取 payload 中 eventId，解析失败回退 Kafka key+offset 摘要（防御性）
        try {
            String topic = record.topic();
            TspViamProjectionProperties.Topics topics = properties.getConsumer().getTopics();
            if (topics.getLoginSucceeded().equals(topic)) {
                return objectMapper.readValue(record.value(), DeviceLoginSucceededEvent.class).eventId();
            }
            if (topics.getOnboardingChanged().equals(topic)) {
                return objectMapper.readValue(record.value(), DeviceOnboardingChangedEvent.class).eventId();
            }
            if (topics.getAccessInvalidated().equals(topic)) {
                return objectMapper.readValue(record.value(), AccessInvalidatedEvent.class).getEventId();
            }
            if (topics.getV2cKeyProvisioned().equals(topic)) {
                return objectMapper.readValue(record.value(), BusinessKeyChangedEvent.class).getEventId();
            }
        } catch (Exception ignored) {
            // 解析失败由 processOnce 抛错走重试
        }
        return "fallback:" + record.topic() + ":" + record.offset();
    }

    private String principalIdOf(String topic, String payload) {
        try {
            TspViamProjectionProperties.Topics topics = properties.getConsumer().getTopics();
            if (topics.getLoginSucceeded().equals(topic)) {
                return objectMapper.readValue(payload, DeviceLoginSucceededEvent.class).principalId();
            }
            if (topics.getOnboardingChanged().equals(topic)) {
                return objectMapper.readValue(payload, DeviceOnboardingChangedEvent.class).principalId();
            }
            if (topics.getAccessInvalidated().equals(topic)) {
                return objectMapper.readValue(payload, AccessInvalidatedEvent.class).getPrincipalId();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String hsmUidOf(String topic, String payload) {
        try {
            TspViamProjectionProperties.Topics topics = properties.getConsumer().getTopics();
            if (topics.getLoginSucceeded().equals(topic)) {
                return objectMapper.readValue(payload, DeviceLoginSucceededEvent.class).hsmUid();
            }
            if (topics.getOnboardingChanged().equals(topic)) {
                return objectMapper.readValue(payload, DeviceOnboardingChangedEvent.class).hsmUid();
            }
            if (topics.getAccessInvalidated().equals(topic)) {
                return objectMapper.readValue(payload, AccessInvalidatedEvent.class).getHsmUid();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String deviceSnOf(String topic, String payload) {
        try {
            TspViamProjectionProperties.Topics topics = properties.getConsumer().getTopics();
            if (topics.getOnboardingChanged().equals(topic)) {
                return objectMapper.readValue(payload, DeviceOnboardingChangedEvent.class).deviceSn();
            }
            if (topics.getAccessInvalidated().equals(topic)) {
                return objectMapper.readValue(payload, AccessInvalidatedEvent.class).getDeviceSn();
            }
            if (topics.getV2cKeyProvisioned().equals(topic)) {
                return objectMapper.readValue(payload, BusinessKeyChangedEvent.class).getDeviceSn();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String eventTypeOf(String topic) {
        TspViamProjectionProperties.Topics topics = properties.getConsumer().getTopics();
        if (topics.getLoginSucceeded().equals(topic)) {
            return DeviceLoginSucceededEvent.EVENT_TYPE;
        }
        if (topics.getOnboardingChanged().equals(topic)) {
            return DeviceOnboardingChangedEvent.EVENT_TYPE;
        }
        if (topics.getAccessInvalidated().equals(topic)) {
            return AccessInvalidatedEvent.EVENT_TYPE;
        }
        if (topics.getV2cKeyProvisioned().equals(topic)) {
            return BusinessKeyChangedEvent.EVENT_TYPE;
        }
        return "UNKNOWN";
    }

    private Duration backoff(int attempt) {
        long initialMs = properties.getConsumer().getRetry().getInitialBackoff().toMillis();
        long factor = 1L << Math.min(attempt - 1, 20);
        long millis = initialMs > Long.MAX_VALUE / factor ? Long.MAX_VALUE : initialMs * factor;
        long capped = Math.min(millis, properties.getConsumer().getRetry().getMaxBackoff().toMillis());
        return Duration.ofMillis(Math.max(capped, 0));
    }

    private String sha256(String payload) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private String safeSummary(Throwable e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }

    private String mask(String value) {
        if (value == null || value.length() <= 8) {
            return value;
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
