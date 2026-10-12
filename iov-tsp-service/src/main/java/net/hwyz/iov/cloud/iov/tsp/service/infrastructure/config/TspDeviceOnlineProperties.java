package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * TSP DEVICE_ONLINE 消费者配置（TSP-DSN-CR-010 §2）
 * <p>
 * {@code tsp.kafka.device-online.*}：Topic、独立 Consumer Group、重试策略与 DLT。
 * Topic 名从校验配置读取，不在 Java 常量中硬编码。
 *
 * @author hwyz_leo
 */
@Data
@Component
@ConfigurationProperties(prefix = "tsp.kafka.device-online")
public class TspDeviceOnlineProperties {

    /**
     * 是否启用 DEVICE_ONLINE 消费者
     */
    private boolean enabled = true;

    /**
     * 源 Topic（VAGW 生产，TSP 仅消费）
     */
    private String topic = "vagw.device.online";

    /**
     * 独立 Consumer Group，不得与远控/VMD key-provisioned 等共享
     */
    private String groupId = "iov-tsp-device-online";

    /**
     * 单订阅并发（默认 1，保持同分区按序处理）
     */
    private int concurrency = 1;

    /**
     * 重试策略
     */
    private Retry retry = new Retry();

    /**
     * DLT 配置
     */
    private Dlt dlt = new Dlt();

    /**
     * 瞬时异常进程内指数退避重试策略
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
        private String topic = "tsp.device-online.dlt";
        /**
         * 分区数（建议与源 Topic 一致）
         */
        private int partitions = 6;
        /**
         * 副本数
         */
        private short replicationFactor = 3;
    }
}
