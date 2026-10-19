package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinitionProvider;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspTboxV2cKeyProvisionedProperties;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * TSP V2C 聚合成功事件失败隔离 DLT Topic 定义提供者（TSP-DSN-CR-012 §8）
 * <p>
 * TSP 是 {@code tsp.tbox-v2c-key-provisioned.dlt} 的 Producer 与 Provisioning Owner；
 * {@code vmd.tbox-v2c-key-provisioned} 由 VMD 声明，TSP 只消费、不重复声明。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class TspTboxV2cKeyProvisionedDltTopicDefinitionProvider implements KafkaTopicDefinitionProvider {

    private final TspTboxV2cKeyProvisionedProperties properties;

    @Override
    public Collection<KafkaTopicDefinition> topicDefinitions() {
        TspTboxV2cKeyProvisionedProperties.Dlt dlt = properties.getDlt();
        if (dlt == null || !dlt.isEnabled()) {
            return List.of();
        }
        return List.of(new KafkaTopicDefinition(dlt.getTopic(), dlt.getPartitions(), dlt.getReplicationFactor()));
    }
}
