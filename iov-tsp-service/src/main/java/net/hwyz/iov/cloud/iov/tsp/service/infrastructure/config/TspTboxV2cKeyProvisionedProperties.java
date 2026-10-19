package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.event.TboxV2cKeyProvisionedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * TBOX V2C 聚合成功事件消费者配置（TSP-DSN-CR-012 §9/§11）
 * <p>
 * {@code tsp.kafka.tbox-v2c-key-provisioned.*}：Topic、独立 Consumer Group、契约版本、重试策略与 DLT。
 * 发布门禁默认关闭，仅在 VMD CR-062 生产者升级、Topic/ACL 与 E2E 就绪后开启。
 * <p>
 * 迁移期旧键 {@code tsp.tbox-registration.v2c-activation.enabled} 已废弃：
 * 若双键同时存在且取值不一致，启动 fail-fast，避免“误以为已开启/已关闭”的语义漂移。
 *
 * @author hwyz_leo
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "tsp.kafka.tbox-v2c-key-provisioned")
public class TspTboxV2cKeyProvisionedProperties {

    /**
     * 是否启用 V2C 聚合成功事件消费者（发布门禁，默认关闭）
     */
    private boolean enabled = false;

    /**
     * 源 Topic（VMD 生产，TSP 仅消费）
     */
    private String topic = "vmd.tbox-v2c-key-provisioned";

    /**
     * 独立 Consumer Group，不得与 DEVICE_ONLINE / 远控等共享
     */
    private String groupId = "iov-tsp-tbox-v2c-key-provisioned";

    /**
     * 单订阅并发（默认 1，保持同分区按序处理）
     */
    private int concurrency = 1;

    /**
     * 单个事件 value 最大字节数（超限永久拒绝，避免大对象拖垮消费）
     */
    private int maxPayloadBytes = 16384;

    /**
     * Kafka Key / deviceSn 最大字节数
     */
    private int maxDeviceSnBytes = 128;

    /**
     * occurredAt 允许的未来时钟偏移
     */
    private Duration maxFutureSkew = Duration.ofMinutes(5);

    /**
     * 受支持的事件契约版本；未知非空版本 fail-closed
     */
    private Set<String> supportedContractVersions =
            new LinkedHashSet<>(TboxV2cKeyProvisionedEvent.SUPPORTED_CONTRACT_VERSIONS);

    /**
     * 必需的 VMD 生产者 release（TSP-DSN-CR-012 §11 切换门禁）
     */
    private String requiredProducerRelease = "VMD-CR-062";

    /**
     * 当前环境声明的 VMD 生产者 release；启用消费者时必须与 {@link #requiredProducerRelease} 一致
     * <p>
     * v1 契约本身不能证明生产者已升级，必须由部署门禁显式声明；
     * 未声明或版本不符时启动 fail-fast，禁止旧/新生产者混跑时误激活。
     */
    private String activeProducerRelease;

    /**
     * 重试策略
     */
    private Retry retry = new Retry();

    /**
     * DLT 配置
     */
    private Dlt dlt = new Dlt();

    /**
     * 旧发布门禁（废弃，仅用于迁移期冲突检测）
     */
    @Value("${tsp.tbox-registration.v2c-activation.enabled:#{null}}")
    private Boolean legacyV2cActivationEnabled;

    /**
     * 迁移期双键冲突检测：取值不一致即 fail-fast，避免发布门禁语义漂移。
     */
    @PostConstruct
    public void validateLegacyGate() {
        if (legacyV2cActivationEnabled != null) {
            if (legacyV2cActivationEnabled != enabled) {
                throw new IllegalStateException("TSP-DSN-CR-012 配置冲突: 旧键 tsp.tbox-registration.v2c-activation.enabled="
                        + legacyV2cActivationEnabled + " 与新键 tsp.kafka.tbox-v2c-key-provisioned.enabled=" + enabled
                        + " 不一致，请仅保留新键后重启");
            }
            log.warn("旧配置键 tsp.tbox-registration.v2c-activation.enabled 已废弃，请迁移至 "
                    + "tsp.kafka.tbox-v2c-key-provisioned.enabled 并删除旧键");
        }
        validateReleaseGate();
    }

    /**
     * 切换门禁：启用时必须显式声明 VMD 生产者已升级到 CR-062，否则 fail-fast。
     * <p>
     * 切换期间须先暂停消费、排空/隔离旧语义事件，且不允许旧、新 VMD 生产者混跑。
     */
    public void validateReleaseGate() {
        if (!enabled) {
            return;
        }
        if (requiredProducerRelease != null && !requiredProducerRelease.equals(activeProducerRelease)) {
            throw new IllegalStateException("TSP-DSN-CR-012 发布门禁未满足: 需声明 active-producer-release="
                    + requiredProducerRelease + "（当前=" + activeProducerRelease
                    + "）；请确认 VMD 生产者已升级、旧语义事件已暂停/排空且未混跑");
        }
    }

    /**
     * 生产者 release 门禁是否满足（readiness 判定）
     */
    public boolean isProducerReleaseReady() {
        return requiredProducerRelease == null || requiredProducerRelease.equals(activeProducerRelease);
    }

    /**
     * 当前契约版本是否受支持（readiness 判定）
     */
    public boolean isCurrentContractVersionSupported() {
        return supportedContractVersions != null
                && supportedContractVersions.contains(TboxV2cKeyProvisionedEvent.CONTRACT_VERSION_V1);
    }

    /**
     * 瞬时异常/可重试结果的进程内指数退避重试策略。
     * 重试沿用原事件与幂等身份，不生成新 requestId。
     */
    @Data
    public static class Retry {
        /**
         * 最大尝试次数（含首次），耗尽后进入 DLT
         */
        private int maxAttempts = 5;
        /**
         * 首次退避
         */
        private Duration initialBackoff = Duration.ofSeconds(1);
        /**
         * 退避封顶
         */
        private Duration maxBackoff = Duration.ofSeconds(30);
    }

    /**
     * 失败隔离 DLT：TSP 为该 Topic 的 Producer/Provisioning Owner
     */
    @Data
    public static class Dlt {
        /**
         * 是否启用 DLT 发布
         */
        private boolean enabled = true;
        /**
         * DLT Topic（TSP 自产）
         */
        private String topic = "tsp.tbox-v2c-key-provisioned.dlt";
        /**
         * 分区数（建议与源 Topic 一致）
         */
        private int partitions = 6;
        /**
         * 副本数
         */
        private short replicationFactor = 3;

        /**
         * DLT 保留期（天）：事件含 keyId，需受控保留与访问
         */
        private int retentionDays = 7;
    }
}
