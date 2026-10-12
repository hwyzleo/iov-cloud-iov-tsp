package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * TBOX 后台管理配置
 * <p>
 * TSP-DSN-CR-009：仅影响异常标签派生与查询/导出边界，不改变设备状态。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tsp.tbox-management")
public class TboxManagementProperties {

    /**
     * 激活超时阈值（self-describing ISO-8601，如 PT30M）
     */
    private Duration activationTimeout = Duration.ofMinutes(30);

    /**
     * 长期未登录阈值（如 P30D）
     */
    private Duration longOfflineThreshold = Duration.ofDays(30);

    /**
     * 导出最大行数
     */
    private int exportMaxRows = 10000;

    /**
     * 默认分页大小
     */
    private int defaultPageSize = 20;

    /**
     * 最大分页大小
     */
    private int maxPageSize = 200;
}
