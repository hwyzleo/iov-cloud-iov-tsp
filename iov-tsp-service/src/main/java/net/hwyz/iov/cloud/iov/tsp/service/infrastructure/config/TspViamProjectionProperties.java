package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * TSP VIAM 安全投影消费配置（IOV-TSP-DSN-CR-016）
 * <p>
 * {@code tsp.viam-projection.*}：Topic 清单、独立 Consumer Group、重试策略与快照刷新开关。
 * Topic 名从校验配置读取，不在 Java 常量中硬编码；默认值对齐 VIAM 官方契约
 * （{@code viam.device.login-succeeded} 等，schema/owner/ACL 登记后方可启用）。
 * </p>
 *
 * @author hwyz_leo
 */
@Data
@Component
@ConfigurationProperties(prefix = "tsp.viam-projection")
public class TspViamProjectionProperties {

    /**
     * 是否启用 VIAM 安全投影消费（旧 TSP 安全写链停用后启用）
     */
    private boolean enabled = false;

    /**
     * 消费者配置
     */
    private Consumer consumer = new Consumer();

    /**
     * 快照刷新配置（VIAM 官方 onboarding 快照查询）
     */
    private Snapshot snapshot = new Snapshot();

    /**
     * 消费者配置
     */
    @Data
    public static class Consumer {

        /**
         * 独立 Consumer Group（VIAM 事件消费端）
         */
        private String groupId = "iov-tsp-viam-projection";

        /**
         * 事件 Topic（对齐 VIAM 官方契约默认值）
         */
        private Topics topics = new Topics();

        /**
         * 瞬时异常进程内指数退避重试策略
         */
        private Retry retry = new Retry();

        /**
         * 单订阅并发（默认 1，保持同分区按序处理）
         */
        private int concurrency = 1;
    }

    /**
     * 事件 Topic 契约
     */
    @Data
    public static class Topics {
        /**
         * 成功接入事实事件（独立更新最近登录事实）
         */
        private String loginSucceeded = "viam.device.login-succeeded";
        /**
         * 安全开通状态变更事件（快照刷新触发，不直接推断状态）
         */
        private String onboardingChanged = "viam.device.onboarding.changed";
        /**
         * 访问失效事件（快照刷新触发，不按 reason 推断状态）
         */
        private String accessInvalidated = "viam.access-invalidated";
        /**
         * V2C 密钥开通完成事件（密钥开通里程碑；activate_time 语义待 VIAM 契约明确）
         */
        private String v2cKeyProvisioned = "viam.tbox-v2c-key-provisioned";
    }

    /**
     * 瞬时异常进程内指数退避重试策略
     */
    @Data
    public static class Retry {
        /**
         * 最大尝试次数（含首次）
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
     * 快照刷新配置
     */
    @Data
    public static class Snapshot {
        /**
         * 是否启用 VIAM onboarding 快照刷新（事件触发时按主体合并刷新）
         */
        private boolean enabled = true;
        /**
         * 快照刷新失败重试次数（超时/依赖不可用；fail-closed，不伪装状态）
         */
        private int maxAttempts = 3;
        /**
         * 快照刷新初始退避
         */
        private Duration initialBackoff = Duration.ofSeconds(1);
        /**
         * 快照刷新退避封顶
         */
        private Duration maxBackoff = Duration.ofSeconds(10);
    }
}
