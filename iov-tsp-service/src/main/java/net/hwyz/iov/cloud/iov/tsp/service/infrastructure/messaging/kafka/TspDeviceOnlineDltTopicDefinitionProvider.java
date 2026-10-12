package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinitionProvider;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspDeviceOnlineProperties;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * TSP DEVICE_ONLINE 失败隔离 DLT Topic 定义提供者（TSP-DSN-CR-010 R1）
 * <p>
 * TSP 是 {@code tsp.device-online.dlt} 的 Producer 与 Provisioning Owner；
 * {@code vagw.device.online} 由 VAGW 声明，TSP 只消费、不重复声明。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class TspDeviceOnlineDltTopicDefinitionProvider implements KafkaTopicDefinitionProvider {

    private final TspDeviceOnlineProperties properties;

    @Override
    public Collection<KafkaTopicDefinition> topicDefinitions() {
        TspDeviceOnlineProperties.Dlt dlt = properties.getDlt();
        if (dlt == null || !dlt.isEnabled()) {
            return List.of();
        }
        return List.of(new KafkaTopicDefinition(dlt.getTopic(), dlt.getPartitions(), dlt.getReplicationFactor()));
    }
}
