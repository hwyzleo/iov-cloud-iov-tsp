package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户车辆关系模块配置（IOV-TSP-DSN-CR-017）。
 * <p>
 * TTL/恢复窗口是必须显式配置的业务参数；客户端类型取决于 SGW/CIAM 令牌声明，
 * 不在代码中硬编码车机类型。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tsp.user-vehicle")
public class TspUserVehicleProperties {

    /**
     * 二维码会话有效期（秒级精度由 DATETIME(3) 承载）
     */
    private Duration sessionTtl = Duration.ofMinutes(10);

    /**
     * 车端绑定结果恢复窗口
     */
    private Duration recoveryWindow = Duration.ofMinutes(30);

    /**
     * App 客户端类型（SGW 令牌 client_type 声明）
     */
    private String appClientType = "MOBILE";

    /**
     * 车机客户端类型（SGW 令牌 client_type 声明，车辆会话必须携带 vehicle_id）
     */
    private String vehicleClientType = "IVI";

    /**
     * 协议默认语言（CMS 缺省时按服务端规范）
     */
    private String defaultLanguage = "zh-CN";

    /**
     * 协议场景（MKT-CMS 发布包 scene）
     */
    private String agreementScene = "IVI_ACTIVATION";

    /**
     * 首次激活候选事实投递配置
     */
    private ActivationFact activationFact = new ActivationFact();

    /**
     * 一次性登录票据配置（IOV-TSP-DSN-CR-019 D019-02/D019-04）
     */
    private LoginTicket loginTicket = new LoginTicket();

    /**
     * 隐私授权项目录与上报强校验（IOV-TSP-DSN-CR-020 D020-01/D020-03）
     */
    private PrivacyAuthorization privacyAuthorization = new PrivacyAuthorization();

    /**
     * 车端会话创建幂等台账（IOV-TSP-DSN-CR-020 D020-04/D020-05）
     */
    private SessionIdempotency sessionIdempotency = new SessionIdempotency();

    /**
     * 隐私授权项目录与强校验配置。
     * <p>
     * 目录是授权事实的唯一权威（不落库、不使用 CMS）：任何条目内容变更都必须同步变更
     * {@code catalog-version}，否则旧版本上报将持续命中 401119（运维发布检查项）。
     */
    @Getter
    @Setter
    public static class PrivacyAuthorization {

        /**
         * 是否启用目录端点与上报强校验（false 仅用于回滚；目录本身仍必须合法）
         */
        private boolean enabled = true;

        /**
         * 是否强制校验目录（false 时上报只告警不拒绝，仅回滚用，生产不得长期开启）
         */
        private boolean enforceCatalog = true;

        /**
         * 目录版本（上报必须原样回传，否则 401119；目录任何变更都要改此值）
         */
        private String catalogVersion;

        /**
         * 目录条目（结构占位；真实取值待产品/PRD 确认）
         */
        private List<Item> items = new ArrayList<>();

        /**
         * 单个目录条目
         */
        @Getter
        @Setter
        public static class Item {

            /**
             * 授权项编码（^[A-Z][A-Z0-9_]{2,63}$，区分大小写）
             */
            private String item;

            /**
             * 默认语言（zh-CN）展示名
             */
            private String displayName;

            /**
             * 英文（en-US）展示名
             */
            private String displayNameEn;

            /**
             * 是否必选授权项
             */
            private boolean required;
        }
    }

    /**
     * 车端会话创建幂等台账配置
     */
    @Getter
    @Setter
    public static class SessionIdempotency {

        /**
         * 是否启用 Idempotency-Key 幂等（false 为回滚开关：不读取请求头，行为回到「每次新建」）
         */
        private boolean enabled = true;

        /**
         * 幂等键最大长度（与 V19 idempotency_key VARCHAR(128) 对齐）
         */
        private int maxKeyLength = 128;
    }

    /**
     * 一次性登录票据配置
     * <p>
     * TTL 短时效 + 会话级签发次数/频率上限是最小泄露窗口与重放面的一部分，
     * 阈值可按联调确认调整；enabled=false 仅停止签发（回滚开关），不清理已签发票据。
     */
    @Getter
    @Setter
    public static class LoginTicket {

        /**
         * 是否允许签发（false 为回滚开关；已签发未消费票据仍可消费至自然过期）
         */
        private boolean enabled = true;

        /**
         * 票据有效期（默认 180 秒）
         */
        private Duration ttl = Duration.ofSeconds(180);

        /**
         * 同一会话每分钟签发上限（<=0 表示不限制）
         */
        private int perSessionPerMinute = 3;

        /**
         * 同一会话累计签发次数上限（重签上限）
         */
        private int perSessionMaxIssue = 10;

        /**
         * 票据消费方标识（仅作为审计记录的消费方，不做调用方准入判定）
         */
        private String consumer = "sec-ciam";
    }

    /**
     * 首次激活候选事实投递配置
     */
    @Getter
    @Setter
    public static class ActivationFact {

        /**
         * 是否启用候选事实投递（D017-07：契约确认后启用；默认启用并登记 Topic）
         */
        private boolean publishEnabled = true;

        /**
         * 目标 Topic（TSP 生产、VMD 消费，需在 FW-KAFKA Topic 目录登记）
         */
        private String topic = "tsp.user-vehicle-activation-fact";

        /**
         * 目标 Topic 副本因子（终审修复 B-5）：默认 2（生产高可用），
         * 与 {@code tbox-vagw-cmd} 的固定副本因子 1 <b>完全解耦</b>，互不影响。
         * <p>
         * 本地/单 Broker E2E 环境通过环境变量 {@code TSP_ACTIVATION_FACT_REPLICATION_FACTOR=1} 覆盖，
         * 否则单 Broker 集群无法满足副本因子 2（Topic 创建失败 → 预留区准备重试/健康降级）。
         */
        private short replicationFactor = 2;

        /**
         * 目标 Topic 的最小同步副本数（建议 1：2 副本下允许单节点故障；
         * 本地 E2E 副本因子置 1 时仍合法，约束为 1 <= min.insync.replicas <= replicationFactor）
         */
        private short minInsyncReplicas = 1;

        /**
         * 事件契约版本
         */
        private int schemaVersion = 1;

        /**
         * 单次投递批量
         */
        private int batchSize = 50;

        /**
         * 最大重试次数（达到后转 FAILED 终态，保留事实可人工重放）
         */
        private int maxRetry = 10;

        /**
         * Outbox 轮询间隔（毫秒）
         */
        private long pollIntervalMs = 10000L;
    }
}
