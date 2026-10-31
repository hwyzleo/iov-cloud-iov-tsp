package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import com.alibaba.druid.pool.DruidDataSource;
import net.hwyz.iov.cloud.framework.mysql.time.MysqlTimeAutoConfiguration;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.ZoneId;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * TSP-DSN-CR-014：FW-MYSQL UTC 治理探针完整通过链真实 MySQL 验证。
 * <p>
 * 前提：本地 docker 容器 tsp-cr014-mysql（端口 13306，库 db_tsp_cr014，已执行 V1~V15）。
 * 运行（必须 UTC JVM，否则探针按设计拒绝）：
 * <pre>
 *   mvn -o test -pl iov-tsp-service -Dtest=Cr014ProbePassIt -Duser.timezone=UTC
 * </pre>
 * 验证：enabled=true + JVM UTC + URL 参数（connectionTimeZone/forceConnectionTimeZoneToSession/preserveInstants）
 * + Druid 会话初始化（SET time_zone='+00:00'）→ 探针真实连接校验通过，上下文启动成功。
 * 类名以 It 结尾，surefire 默认不执行；非 UTC JVM 或 DB 不可用时跳过。
 *
 * @author hwyz_leo
 * @since 2026-10-31
 */
class Cr014ProbePassIt {

    @Test
    void probeEnabled_WithUtcJvmAndFullDruidConfig_ShouldPass() {
        Assumptions.assumeTrue(isUtcEquivalent(TimeZone.getDefault().getID()),
                "探针要求 JVM/OS UTC，本 JVM=" + TimeZone.getDefault().getID());

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MysqlTimeAutoConfiguration.class))
                .withPropertyValues("iov.mysql.time.enabled=true")
                .withBean(DruidDataSource.class, () -> {
                    DruidDataSource ds = new DruidDataSource();
                    ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
                    ds.setUrl("jdbc:mysql://localhost:13306/db_tsp_cr014"
                            + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8"
                            + "&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&preserveInstants=true");
                    ds.setUsername("root");
                    ds.setPassword("cr014_root_pw");
                    ds.setConnectionInitSqls(List.of("SET time_zone='+00:00'"));
                    return ds;
                })
                .run(context -> assertFalse(context.getStartupFailure() != null,
                        () -> "探针应通过（JVM UTC + URL 参数 + initSql + 真实连接会话）："
                                + (context.getStartupFailure() == null ? null : context.getStartupFailure().getMessage())));
    }

    private static boolean isUtcEquivalent(String id) {
        if (id == null) {
            return false;
        }
        String normalized = id.toLowerCase();
        ZoneId zone;
        try {
            zone = ZoneId.of(id);
        } catch (Exception e) {
            return false;
        }
        return zone.getRules().getOffset(java.time.Instant.EPOCH).getTotalSeconds() == 0
                && zone.getRules().isFixedOffset();
    }
}
