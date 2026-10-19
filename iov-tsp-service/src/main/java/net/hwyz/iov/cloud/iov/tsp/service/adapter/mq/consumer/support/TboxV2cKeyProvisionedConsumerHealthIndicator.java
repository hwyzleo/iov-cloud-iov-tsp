package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.kafka.support.KafkaAdminOperations;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxRegistrationEventRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspTboxV2cKeyProvisionedProperties;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * V2C 激活专用健康检查（TSP-DSN-CR-012 §9）
 * <p>
 * readiness = topic/ACL ready AND consumer group ready AND event contract version supported
 * AND database/inbox ready AND retry/DLT publisher ready。
 * 不直接探测 FW-SEC verifier、PAR-PROTO release 或 TBOX-SEC sign capability；
 * 不输出 deviceSn、requestId、keyId 等敏感信息，也不拖垮查询类能力的全局 readiness。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TboxV2cKeyProvisionedConsumerHealthIndicator implements HealthIndicator {

    private static final Duration DESCRIBE_TIMEOUT = Duration.ofSeconds(5);
    private static final String INBOX_PROBE_EVENT_ID = "__keyprov_inbox_probe__";

    private final TspTboxV2cKeyProvisionedProperties properties;
    private final TboxV2cKeyProvisionedConsumerState state;
    private final TboxRegistrationEventRepository tboxRegistrationEventRepository;
    private final ObjectProvider<Admin> adminProvider;
    private final KafkaAdminOperations kafkaAdminOperations;

    @Override
    public Health health() {
        Health.Builder builder = Health.up()
                .withDetail("topic", properties.getTopic())
                .withDetail("groupId", properties.getGroupId())
                .withDetail("enabled", properties.isEnabled())
                .withDetail("started", state.isStarted())
                .withDetail("subscribed", state.isSubscribed())
                .withDetail("contractVersionSupported", properties.isCurrentContractVersionSupported())
                .withDetail("requiredProducerRelease", properties.getRequiredProducerRelease())
                .withDetail("producerReleaseReady", properties.isProducerReleaseReady())
                .withDetail("lastPollAt", String.valueOf(state.getLastPollAt()))
                .withDetail("lastSuccessAt", String.valueOf(state.getLastSuccessAt()))
                .withDetail("retryTotal", state.getRetryTotal())
                .withDetail("dltTotal", state.getDltTotal());

        if (!properties.isEnabled()) {
            return builder.status(Status.OUT_OF_SERVICE)
                    .withDetail("reason", "consumer disabled")
                    .build();
        }
        if (!properties.isCurrentContractVersionSupported()) {
            return builder.down().withDetail("reason", "event contract version unsupported").build();
        }
        if (!properties.isProducerReleaseReady()) {
            return builder.down().withDetail("reason", "producer release gate not satisfied").build();
        }
        boolean dltReady = properties.getDlt() != null && properties.getDlt().isEnabled();
        boolean retryReady = properties.getRetry() != null && properties.getRetry().getMaxAttempts() > 0;
        if (!dltReady || !retryReady) {
            return builder.down()
                    .withDetail("reason", "retry/DLT publisher not ready")
                    .withDetail("dltReady", dltReady)
                    .withDetail("retryReady", retryReady)
                    .build();
        }
        if (!state.isStarted() || !state.isSubscribed()) {
            return builder.down().withDetail("reason", "consumer not subscribed").build();
        }
        if (!isInboxReady()) {
            return builder.down().withDetail("reason", "database/inbox not ready").build();
        }
        Admin admin = adminProvider.getIfAvailable();
        if (admin == null) {
            return builder.down().withDetail("reason", "kafka admin unavailable").build();
        }
        try {
            Map<String, TopicDescription> descriptions = kafkaAdminOperations.describeTopics(
                    admin, List.of(properties.getTopic()), DESCRIBE_TIMEOUT);
            TopicDescription description = descriptions.get(properties.getTopic());
            if (description == null) {
                return builder.down().withDetail("reason", "topic metadata missing").build();
            }
            builder.withDetail("partitionCount", description.partitions().size());

            ConsumerGroupDescription groupDescription = admin
                    .describeConsumerGroups(List.of(properties.getGroupId()))
                    .all()
                    .get(DESCRIBE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                    .get(properties.getGroupId());
            ConsumerGroupState groupState = groupDescription == null ? null : groupDescription.state();
            builder.withDetail("consumerGroupState", groupState == null ? null : groupState.name());
            if (groupState == ConsumerGroupState.EMPTY
                    || groupState == ConsumerGroupState.DEAD
                    || groupState == ConsumerGroupState.UNKNOWN) {
                return builder.down().withDetail("reason", "consumer group not ready").build();
            }
            return builder.up().build();
        } catch (UnknownTopicOrPartitionException e) {
            return builder.down().withDetail("reason", "topic not found").build();
        } catch (Exception e) {
            log.warn("V2C 激活 HealthIndicator 检查失败, errorClass={}", e.getClass().getSimpleName());
            return builder.down().withDetail("reason", "kafka describe failed: " + e.getClass().getSimpleName()).build();
        }
    }

    private boolean isInboxReady() {
        try {
            tboxRegistrationEventRepository.findByEventId(INBOX_PROBE_EVENT_ID);
            return true;
        } catch (Exception e) {
            log.warn("V2C 激活 HealthIndicator Inbox 探测失败, errorClass={}", e.getClass().getSimpleName());
            return false;
        }
    }
}
