package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxManagementCriteria;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TBOX 运营聚合查询动态 SQL 集成测试（TSP-DSN-CR-009，不依赖数据库）
 * <p>
 * 通过 MyBatis 解析 Mapper XML 并渲染 BoundSql，验证过滤、绑定聚合与排序白名单分支被正确拼接。
 */
class TboxManagementQueryMapperSqlTest {

    private static final String MAPPER_XML = "/mappers/TboxManagementQueryMapper.xml";
    private static final String NAMESPACE =
            "net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxManagementQueryMapper";

    private Configuration configuration;

    @BeforeEach
    void setUp() throws Exception {
        configuration = new Configuration();
        try (InputStream inputStream = getClass().getResourceAsStream(MAPPER_XML)) {
            assertNotNull(inputStream, "未找到 " + MAPPER_XML);
            new XMLMapperBuilder(inputStream, configuration, MAPPER_XML, configuration.getSqlFragments()).parse();
        }
    }

    private String renderPageSql(TboxManagementCriteria criteria) {
        MappedStatement statement = configuration.getMappedStatement(NAMESPACE + ".selectPageByCriteria");
        return statement.getBoundSql(criteria).getSql().replaceAll("\\s+", " ");
    }

    @Test
    void defaultCriteria_ShouldAggregateActiveBindingsAndSortByLastLoginDesc() {
        String sql = renderPageSql(TboxManagementCriteria.builder().sortBy("lastLoginTime").build());

        assertTrue(sql.contains("LEFT JOIN"), "缺少绑定投影 LEFT JOIN");
        assertTrue(sql.contains("GROUP BY sn"), "缺少 TBOX grain 聚合");
        assertTrue(sql.contains("bind_state = 'ACTIVE'"), "缺少 ACTIVE 绑定过滤");
        assertTrue(sql.contains("t.last_login_time IS NULL, t.last_login_time DESC"), "默认排序应为最后登录时间倒序且 NULL 最后");
        assertTrue(sql.contains("COALESCE(b.bindCount, 0) AS bindCount"), "缺少绑定数量聚合列");
    }

    @Test
    void unboundBindingStatus_ShouldFilterNullBindCount() {
        String sql = renderPageSql(TboxManagementCriteria.builder()
                .bindingStatus("UNBOUND").sortBy("lastLoginTime").build());

        assertTrue(sql.contains("AND b.bindCount IS NULL"), "UNBOUND 应按无 active 绑定过滤");
    }

    @Test
    void bindingConflict_ShouldFilterMultipleBindCount() {
        String sql = renderPageSql(TboxManagementCriteria.builder()
                .bindingStatus("BINDING_CONFLICT").sortBy("lastLoginTime").build());

        assertTrue(sql.contains("AND b.bindCount > 1"), "BINDING_CONFLICT 应按多条 active 绑定过滤");
    }

    @Test
    void activationTimeoutAnomaly_ShouldFilterActivatingBeforeThreshold() {
        String sql = renderPageSql(TboxManagementCriteria.builder()
                .anomalyType("ACTIVATION_TIMEOUT")
                .activationTimeoutBefore(LocalDateTime.of(2026, 10, 12, 11, 30, 0))
                .sortBy("lastLoginTime").build());

        assertTrue(sql.contains("AND t.device_status = 5"), "激活超时应限定 ACTIVATING");
        assertTrue(sql.contains("AND t.first_login_time < ?"), "激活超时应限定阈值之前");
    }

    @Test
    void neverOnlineAnomaly_ShouldFilterNullFirstLogin() {
        String sql = renderPageSql(TboxManagementCriteria.builder()
                .anomalyType("NEVER_ONLINE").sortBy("lastLoginTime").build());

        assertTrue(sql.contains("AND t.first_login_time IS NULL"), "从未上线应按首次登录为空过滤");
    }

    @Test
    void sortWhitelist_ShouldSwitchColumnAndDirection() {
        String asc = renderPageSql(TboxManagementCriteria.builder()
                .sortBy("deviceStatus").sortAsc(true).build());
        assertTrue(asc.contains("t.device_status ASC"), "deviceStatus 升序排序未生效");

        String created = renderPageSql(TboxManagementCriteria.builder()
                .sortBy("createdAt").sortAsc(false).build());
        assertTrue(created.contains("t.create_time IS NULL, t.create_time DESC"), "createdAt 排序未生效");
    }

    @Test
    void filters_ShouldExposeVinAndPartCodeFilters() {
        String sql = renderPageSql(TboxManagementCriteria.builder()
                .vin("LSVAA2180C2000001").partCode("PART-1").sn("SN-1").build());

        assertTrue(sql.contains("vb.vin = ?"), "缺少 VIN 过滤");
        assertTrue(sql.contains("vb.part_code = ?"), "缺少 partCode 过滤");
        assertTrue(sql.contains("AND t.sn = ?"), "缺少 SN 过滤");
    }

    @Test
    void detailStatement_ShouldFilterBySn() {
        MappedStatement statement = configuration.getMappedStatement(NAMESPACE + ".selectBySn");
        String sql = statement.getBoundSql("SN-1").getSql().replaceAll("\\s+", " ");

        assertTrue(sql.contains("AND t.sn = ?"), "详情查询应按 SN 过滤");
    }
}
