package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TSP Topic 定义测试（终审修复 B-5：激活事实 Topic 副本因子独立可配置）。
 * <p>
 * 断言口径：
 * <ul>
 *   <li>激活事实 Topic 默认副本因子 2（生产高可用），可被配置/环境变量覆盖为 1（本地单 Broker E2E）；</li>
 *   <li>{@code tbox-vagw-cmd} 副本因子恒为 1，不受激活事实配置影响；</li>
 *   <li>激活事实 Topic 声明 {@code min.insync.replicas=1}（且不超过副本因子）；</li>
 *   <li>投递关闭时只声明 {@code tbox-vagw-cmd}。</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@DisplayName("TspCommandTopicDefinitionProvider 测试（B-5 副本因子）")
class TspCommandTopicDefinitionProviderTest {

    private static final String TBOX_TOPIC = "tbox-vagw-cmd";
    private static final String FACT_TOPIC = "tsp.user-vehicle-activation-fact";

    private TspUserVehicleProperties properties(short replicationFactor) {
        TspUserVehicleProperties properties = new TspUserVehicleProperties();
        properties.getActivationFact().setReplicationFactor(replicationFactor);
        return properties;
    }

    private Map<String, KafkaTopicDefinition> definitions(TspUserVehicleProperties properties) {
        return new TspCommandTopicDefinitionProvider(properties).topicDefinitions().stream()
                .collect(Collectors.toMap(KafkaTopicDefinition::name, Function.identity()));
    }

    @Test
    @DisplayName("默认：激活事实 Topic 副本因子 2（与 tbox-vagw-cmd 的 1 解耦）")
    void activationFactTopicDefaultsToReplicationFactorTwo() {
        Map<String, KafkaTopicDefinition> definitions = definitions(new TspUserVehicleProperties());

        assertThat(definitions).containsOnlyKeys(TBOX_TOPIC, FACT_TOPIC);
        assertThat(definitions.get(FACT_TOPIC).replicationFactor()).isEqualTo((short) 2);
        assertThat(definitions.get(FACT_TOPIC).partitions()).isEqualTo(6);
        assertThat(definitions.get(TBOX_TOPIC).replicationFactor()).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("覆盖为 1：本地 E2E（TSP_ACTIVATION_FACT_REPLICATION_FACTOR=1）生效，tbox-vagw-cmd 不受影响")
    void activationFactTopicReplicationFactorOverridableToOne() {
        Map<String, KafkaTopicDefinition> definitions = definitions(properties((short) 1));

        assertThat(definitions.get(FACT_TOPIC).replicationFactor()).isEqualTo((short) 1);
        assertThat(definitions.get(TBOX_TOPIC).replicationFactor()).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("非法配置（<=0）回落默认 2，不影响 tbox-vagw-cmd")
    void activationFactTopicReplicationFactorFallsBackToDefault() {
        Map<String, KafkaTopicDefinition> definitions = definitions(properties((short) 0));

        assertThat(definitions.get(FACT_TOPIC).replicationFactor()).isEqualTo((short) 2);
        assertThat(definitions.get(TBOX_TOPIC).replicationFactor()).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("激活事实 Topic 声明 min.insync.replicas=1，且不超过副本因子")
    void activationFactTopicDeclaresMinInsyncReplicas() {
        assertThat(definitions(new TspUserVehicleProperties()).get(FACT_TOPIC).configs())
                .containsEntry("min.insync.replicas", "1");

        TspUserVehicleProperties local = properties((short) 1);
        assertThat(definitions(local).get(FACT_TOPIC).configs())
                .containsEntry("min.insync.replicas", "1");

        // 配置的 min.insync.replicas 大于副本因子时收敛到副本因子（Kafka 约束 IC <= RF）
        TspUserVehicleProperties oversized = properties((short) 2);
        oversized.getActivationFact().setMinInsyncReplicas((short) 5);
        assertThat(definitions(oversized).get(FACT_TOPIC).configs())
                .containsEntry("min.insync.replicas", "2");
    }

    @Test
    @DisplayName("tbox-vagw-cmd 不携带激活事实的 Topic 级配置")
    void tboxTopicHasNoActivationFactConfigs() {
        assertThat(definitions(new TspUserVehicleProperties()).get(TBOX_TOPIC).configs()).isEmpty();
        assertThat(definitions(new TspUserVehicleProperties()).get(TBOX_TOPIC).partitions()).isEqualTo(6);
    }

    @Test
    @DisplayName("投递关闭：只声明 tbox-vagw-cmd，且其副本因子仍为 1")
    void activationFactTopicAbsentWhenPublishDisabled() {
        TspUserVehicleProperties properties = properties((short) 2);
        properties.getActivationFact().setPublishEnabled(false);

        Map<String, KafkaTopicDefinition> definitions = definitions(properties);

        assertThat(definitions).containsOnlyKeys(TBOX_TOPIC);
        assertThat(definitions.get(TBOX_TOPIC).replicationFactor()).isEqualTo((short) 1);
    }
}
