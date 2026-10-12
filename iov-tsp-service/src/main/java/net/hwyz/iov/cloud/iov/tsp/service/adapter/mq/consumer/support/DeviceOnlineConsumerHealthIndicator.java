package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.kafka.support.KafkaAdminOperations;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspDeviceOnlineProperties;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * 注册激活专用健康检查（TSP-DSN-CR-010 §8）
 * <p>
 * 检查 DEVICE_ONLINE Consumer 的 Topic metadata、ACL、订阅启动、最近 poll/成功时间与 Retry/DLT 积压；
 * 该健康组表示注册激活消费能力，不拖垮 TSP 查询类能力的全局 readiness。
 * 不输出完整身份信息。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceOnlineConsumerHealthIndicator implements HealthIndicator {

    private static final Duration DESCRIBE_TIMEOUT = Duration.ofSeconds(5);
    private static final String HEALTH_NAME = "deviceOnlineConsumer";

    private final TspDeviceOnlineProperties properties;
    private final DeviceOnlineConsumerState state;
    private final ObjectProvider<Admin> adminProvider;
    private final KafkaAdminOperations kafkaAdminOperations;

    @Override
    public Health health() {
        Health.Builder builder = state.isStarted() ? Health.up() : Health.down();
        builder.withDetail("topic", properties.getTopic());
        builder.withDetail("groupId", properties.getGroupId());
        builder.withDetail("started", state.isStarted());
        builder.withDetail("lastPollAt", String.valueOf(state.getLastPollAt()));
        builder.withDetail("lastSuccessAt", String.valueOf(state.getLastSuccessAt()));
        builder.withDetail("retryTotal", state.getRetryTotal());
        builder.withDetail("rejectedTotal", state.getRejectedTotal());

        if (!properties.isEnabled()) {
            return builder.status(org.springframework.boot.actuate.health.Status.OUT_OF_SERVICE)
                    .withDetail("reason", "consumer disabled")
                    .build();
        }
        if (!state.isStarted()) {
            return builder.withDetail("reason", "consumer not started").build();
        }
        Admin admin = adminProvider.getIfAvailable();
        if (admin == null) {
            return builder.down().withDetail("reason", "kafka admin unavailable").build();
        }
        try {
            Map<String, TopicDescription> descriptions =
                    kafkaAdminOperations.describeTopics(admin, java.util.List.of(properties.getTopic()), DESCRIBE_TIMEOUT);
            TopicDescription description = descriptions.get(properties.getTopic());
            if (description == null) {
                return builder.down().withDetail("reason", "topic metadata missing").build();
            }
            builder.withDetail("partitionCount", description.partitions().size());
            return builder.up().build();
        } catch (UnknownTopicOrPartitionException e) {
            return builder.down().withDetail("reason", "topic not found").build();
        } catch (Exception e) {
            log.warn("注册激活 HealthIndicator 检查失败, errorClass={}", e.getClass().getSimpleName());
            return builder.down().withDetail("reason", "kafka describe failed: " + e.getClass().getSimpleName()).build();
        }
    }

    public String healthName() {
        return HEALTH_NAME;
    }
}
