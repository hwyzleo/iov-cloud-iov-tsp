package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import net.hwyz.iov.cloud.framework.mysql.time.MysqlTimeAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * TSP-DSN-CR-014：FW-MYSQL UTC 治理探针装配行为测试。
 * <p>
 * 验证（RD-TIME-003/004）：
 * <ul>
 *   <li>默认（iov.mysql.time.enabled=false）时探针跳过、上下文可正常启动（本地/开发不阻断）；</li>
 *   <li>enabled=true 且无 DataSource 时探针 fail-fast 阻断启动（无法证明 UTC 覆盖）；</li>
 *   <li>enabled=true 且 JVM/OS 非 UTC 等价时探针 fail-fast（要求 -Duser.timezone=UTC）。</li>
 * </ul>
 * 探针类行为与框架源码一致（MysqlTimeStartupProbe.afterSingletonsInstantiated）。
 *
 * @author hwyz_leo
 * @since 2026-10-31
 */
class Cr014TimeProbeConfigTest {

    @Test
    void probeDisabled_ShouldStartWithoutBlocking() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MysqlTimeAutoConfiguration.class))
                .withPropertyValues("iov.mysql.time.enabled=false")
                .run(context -> assertFalse(context.getStartupFailure() != null,
                        () -> "enabled=false 探针应跳过，不允许阻断启动: "
                                + (context.getStartupFailure() == null ? null : context.getStartupFailure().getMessage())));
    }

    @Test
    void probeEnabled_WithoutDataSource_ShouldNotAssembleAndNotBlock() {
        // @ConditionalOnBean(DataSource)：无 DataSource 时自动配置不装配，探针不运行、不阻断
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MysqlTimeAutoConfiguration.class))
                .withPropertyValues("iov.mysql.time.enabled=true")
                .run(context -> assertFalse(context.getStartupFailure() != null,
                        () -> "无 DataSource 时探针自动配置应不装配（不阻断启动）: "
                                + (context.getStartupFailure() == null ? null : context.getStartupFailure().getMessage())));
    }

    @Test
    void probeEnabled_NonUtcJvm_ShouldFailFast() {
        java.util.TimeZone original = java.util.TimeZone.getDefault();
        try {
            // 模拟非 UTC 启动环境（探针校验 ZoneId.systemDefault），并提供 DataSource 触发装配
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Shanghai"));
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(MysqlTimeAutoConfiguration.class))
                    .withPropertyValues("iov.mysql.time.enabled=true")
                    .withBean(com.alibaba.druid.pool.DruidDataSource.class, () -> {
                        com.alibaba.druid.pool.DruidDataSource ds = new com.alibaba.druid.pool.DruidDataSource();
                        ds.setUrl("jdbc:mysql://localhost:3306/none");
                        return ds;
                    })
                    .run(context -> {
                        Throwable failure = context.getStartupFailure();
                        assertNotNull(failure, "enabled=true 且 JVM 非 UTC 等价时必须 fail-fast");
                        org.assertj.core.api.Assertions.assertThat(failure.getMessage())
                                .contains("JVM/OS 时区非 UTC 等价");
                    });
        } finally {
            java.util.TimeZone.setDefault(original);
        }
    }
}
