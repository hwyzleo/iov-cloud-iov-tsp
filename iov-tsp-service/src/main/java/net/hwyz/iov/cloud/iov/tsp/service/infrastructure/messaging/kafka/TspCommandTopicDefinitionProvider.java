package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinitionProvider;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * TSP Topic 定义提供者（IOV-TSP-DSN-CR-016 保留）
 * <p>
 * TSP 作为 Producer 与 Provisioning Owner 的 Topic 仅剩远控命令 {@code tbox-vagw-cmd}
 * （TboxCmdProducer 生产）。旧安全链路 DLT topic（tsp.device-online.dlt /
 * tsp.tbox-v2c-key-provisioned.dlt）已随旧安全消费者删除，不再声明。
 * 消费侧 topic（ccs.sim-status.changed / vmd.vehicle-part-binding.changed / viam.*）
 * 分别由 CCS/VMD/VIAM 声明，TSP 只消费、不重复声明。
 * </p>
 * <p>
 * 分区/副本口径对齐既有 DLT 配置（6 分区 / 本地副本 1），远控命令按 VIN 分区保证同 VIN 有序。
 * </p>
 *
 * @author hwyz_leo
 */
@Component
public class TspCommandTopicDefinitionProvider implements KafkaTopicDefinitionProvider {

    /** 与 {@link net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.producer.TboxCmdProducer} 生产端保持一致 */
    private static final String TOPIC_TBOX_VAGW_CMD = "tbox-vagw-cmd";
    private static final int PARTITIONS = 6;
    private static final short REPLICATION_FACTOR = 1;

    @Override
    public Collection<KafkaTopicDefinition> topicDefinitions() {
        return List.of(new KafkaTopicDefinition(TOPIC_TBOX_VAGW_CMD, PARTITIONS, REPLICATION_FACTOR));
    }
}
