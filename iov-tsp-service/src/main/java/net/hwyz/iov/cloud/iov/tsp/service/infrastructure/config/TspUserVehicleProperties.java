package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

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
