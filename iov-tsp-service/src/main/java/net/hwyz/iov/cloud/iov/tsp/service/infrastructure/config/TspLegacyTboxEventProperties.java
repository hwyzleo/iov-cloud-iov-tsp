package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 旧 TBOX 聚合事件消费者配置（TSP-DSN-CR-010 §7）
 * <p>
 * {@code tsp.kafka.legacy-tbox-event.*}：默认关闭，不对不存在的旧 Topic 发起 metadata 请求。
 * CMD_ACK/FIND_VEHICLE 由后续 TSP-CR-011 / EDD-VAGW-CR-013 恢复。
 *
 * @author hwyz_leo
 */
@Data
@Component
@ConfigurationProperties(prefix = "tsp.kafka.legacy-tbox-event")
public class TspLegacyTboxEventProperties {

    /**
     * 是否启用旧 TboxEventConsumer（默认 false）
     */
    private boolean enabled = false;

    /**
     * 旧 Topic 名（仅用于日志定位，启用时才会订阅）
     */
    private String topic = "vagw-tbox-event";
}
