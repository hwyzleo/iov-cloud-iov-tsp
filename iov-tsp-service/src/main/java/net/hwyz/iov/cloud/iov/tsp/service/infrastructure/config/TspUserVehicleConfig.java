package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * 用户车辆关系模块基础配置（IOV-TSP-DSN-CR-017）。
 * <p>
 * 服务端到期判定必须使用可测 Clock；本配置同时启用定时任务，
 * 供首次激活候选事实 Outbox 投递重试使用（既有 @Scheduled 对账任务同时生效）。
 *
 * @author hwyz_leo
 */
@Configuration
@EnableScheduling
public class TspUserVehicleConfig {

    /**
     * 业务时钟（UTC），便于单元测试注入固定时刻
     *
     * @return 时钟
     */
    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock tspUserVehicleClock() {
        return Clock.systemUTC();
    }
}
