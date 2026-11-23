package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinitionProvider;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * 分区/副本口径对齐既有 DLT 配置（6 分区）；远控命令按 VIN 分区保证同 VIN 有序。
 * </p>
 * <p>
 * 终审修复 B-5：首次激活候选事实 Topic 的副本因子<b>独立可配置</b>（{@code tsp.user-vehicle.activation-fact.
 * replication-factor}，默认 2，可用环境变量 {@code TSP_ACTIVATION_FACT_REPLICATION_FACTOR} 覆盖为 1），
 * 不再与 {@code tbox-vagw-cmd} 共用写死的常量 1；两个 Topic 的副本因子互不影响。
 * </p>
 *
 * @author hwyz_leo
 */
@Component
public class TspCommandTopicDefinitionProvider implements KafkaTopicDefinitionProvider {

    /** 与 {@link net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.producer.TboxCmdProducer} 生产端保持一致 */
    private static final String TOPIC_TBOX_VAGW_CMD = "tbox-vagw-cmd";
    private static final int PARTITIONS = 6;

    /**
     * 远控命令 Topic 副本因子：保持既有值 1（不得因激活事实 Topic 的调整而变更，
     * 终审修复 B-5 要求「不得影响 tbox-vagw-cmd」）
     */
    private static final short TBOX_VAGW_CMD_REPLICATION_FACTOR = 1;

    /**
     * 激活事实 Topic 副本因子兜底值（配置缺失/非法时使用，与属性默认值一致）
     */
    private static final short ACTIVATION_FACT_DEFAULT_REPLICATION_FACTOR = 2;

    /**
     * 最小同步副本数字段名（Kafka Topic 级配置）
     */
    private static final String MIN_INSYNC_REPLICAS = "min.insync.replicas";

    private final TspUserVehicleProperties userVehicleProperties;

    public TspCommandTopicDefinitionProvider(TspUserVehicleProperties userVehicleProperties) {
        this.userVehicleProperties = userVehicleProperties;
    }

    /**
     * TSP 作为 Producer 的 Topic：
     * <ul>
     *   <li>{@code tbox-vagw-cmd}：远控下行命令（副本因子固定 1，不随激活事实配置变化）；</li>
     *   <li>{@code tsp.user-vehicle-activation-fact}：用户绑车首次激活候选事实（IOV-TSP-DSN-CR-017，
     *       默认启用，需在 FW-KAFKA Topic 目录登记；VMD 为消费者）。副本因子取独立配置，默认 2，
     *       本地 E2E 可用 {@code TSP_ACTIVATION_FACT_REPLICATION_FACTOR=1} 覆盖。</li>
     * </ul>
     *
     * @return Topic 定义集合
     */
    @Override
    public Collection<KafkaTopicDefinition> topicDefinitions() {
        List<KafkaTopicDefinition> definitions = new ArrayList<>();
        definitions.add(new KafkaTopicDefinition(TOPIC_TBOX_VAGW_CMD, PARTITIONS, TBOX_VAGW_CMD_REPLICATION_FACTOR));
        TspUserVehicleProperties.ActivationFact activationFact = userVehicleProperties.getActivationFact();
        if (activationFact.isPublishEnabled()) {
            short replicationFactor = resolveReplicationFactor(activationFact);
            definitions.add(new KafkaTopicDefinition(activationFact.getTopic(), PARTITIONS, replicationFactor,
                    topicConfigs(activationFact, replicationFactor)));
        }
        return definitions;
    }

    /**
     * 解析激活事实 Topic 副本因子：<=0 视为未配置，回落到默认 2
     *
     * @param activationFact 激活事实配置
     * @return 有效副本因子（>=1）
     */
    private short resolveReplicationFactor(TspUserVehicleProperties.ActivationFact activationFact) {
        short configured = activationFact.getReplicationFactor();
        return configured > 0 ? configured : ACTIVATION_FACT_DEFAULT_REPLICATION_FACTOR;
    }

    /**
     * 激活事实 Topic 级配置：{@code min.insync.replicas} 建议 1（2 副本下容忍单节点故障），
     * 且必须满足 {@code 1 <= min.insync.replicas <= replicationFactor}（本地副本因子置 1 时仍合法）。
     *
     * @param activationFact    激活事实配置
     * @param replicationFactor 已解析的副本因子
     * @return Topic 级配置（不可变）
     */
    private Map<String, String> topicConfigs(TspUserVehicleProperties.ActivationFact activationFact,
                                             short replicationFactor) {
        short configured = activationFact.getMinInsyncReplicas();
        short minInsync = configured < 1 ? 1 : (configured > replicationFactor ? replicationFactor : configured);
        Map<String, String> configs = new LinkedHashMap<>();
        configs.put(MIN_INSYNC_REPLICAS, String.valueOf(minInsync));
        return Map.copyOf(configs);
    }
}
