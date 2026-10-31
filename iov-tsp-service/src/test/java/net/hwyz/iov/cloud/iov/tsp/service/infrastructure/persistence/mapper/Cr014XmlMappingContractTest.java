package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.typehandler.UtcInstantTypeHandler;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TSP-DSN-CR-014：XML 双向 UTC 映射 + 审计单属主契约测试（不依赖数据库连接）。
 * <p>
 * 覆盖：
 * <ul>
 *   <li>VehicleTboxMapper.xml / VehicleCcpMapper.xml：last_event_time（Instant）读结果映射 typeHandler 与写参数 typeHandler；</li>
 *   <li>DeviceAdmissionLogMapper.xml：checked_at（Instant）读结果映射与写参数 typeHandler；</li>
 *   <li>全部审计写入口（insert/update/logicalDelete/业务 SQL）不再显式 now() 写入 create/modify_time，
 *       改由 DB DEFAULT/ON UPDATE 唯一生成（审计单属主，RD-TIME-005）。</li>
 * </ul>
 *
 * @author hwyz_leo
 * @since 2026-10-31
 */
class Cr014XmlMappingContractTest {

    private static final String VEHICLE_TBOX_XML = "/mappers/VehicleTboxMapper.xml";
    private static final String VEHICLE_TBOX_NS = "net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleTboxMapper";
    private static final String VEHICLE_CCP_XML = "/mappers/VehicleCcpMapper.xml";
    private static final String VEHICLE_CCP_NS = "net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleCcpMapper";
    private static final String ADMISSION_LOG_XML = "/mappers/DeviceAdmissionLogMapper.xml";
    private static final String ADMISSION_LOG_NS = "net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.DeviceAdmissionLogMapper";

    // ---------- 读结果映射（resultMap typeHandler） ----------

    @Test
    void vehicleTboxResultMap_LastEventTime_ShouldUseUtcInstantTypeHandler() throws Exception {
        Configuration configuration = new Configuration();
        parse(configuration, VEHICLE_TBOX_XML);
        assertResultMapTypeHandler(configuration, VEHICLE_TBOX_NS + ".baseResultMap", "lastEventTime");
    }

    @Test
    void vehicleCcpResultMap_LastEventTime_ShouldUseUtcInstantTypeHandler() throws Exception {
        Configuration configuration = new Configuration();
        parse(configuration, VEHICLE_CCP_XML);
        assertResultMapTypeHandler(configuration, VEHICLE_CCP_NS + ".baseResultMap", "lastEventTime");
    }

    @Test
    void admissionLogResultMap_CheckedAt_ShouldUseUtcInstantTypeHandler() throws Exception {
        Configuration configuration = new Configuration();
        parse(configuration, ADMISSION_LOG_XML);
        assertResultMapTypeHandler(configuration, ADMISSION_LOG_NS + ".baseResultMap", "checkedAt");
    }

    private void assertResultMapTypeHandler(Configuration configuration, String resultMapId, String property) {
        org.apache.ibatis.mapping.ResultMap resultMap = configuration.getResultMap(resultMapId);
        assertNotNull(resultMap, "缺少 resultMap: " + resultMapId);
        ResultMapping mapping = resultMap.getResultMappings().stream()
                .filter(m -> property.equals(m.getProperty()))
                .findFirst()
                .orElse(null);
        assertNotNull(mapping, "缺少列映射 property=" + property);
        assertTrue(mapping.getTypeHandler() instanceof UtcInstantTypeHandler,
                property + " 未使用 UtcInstantTypeHandler，实际=" + mapping.getTypeHandler());
    }

    // ---------- 写参数 typeHandler（insert/upsert，经 parameterMappings 校验） ----------

    @Test
    void vehicleTboxUpsert_LastEventTime_ShouldUseUtcTypeHandlerOnParameter() throws Exception {
        assertParameterTypeHandler(VEHICLE_TBOX_XML, "upsertByBindingId", "lastEventTime");
    }

    @Test
    void vehicleCcpUpsert_LastEventTime_ShouldUseUtcTypeHandlerOnParameter() throws Exception {
        assertParameterTypeHandler(VEHICLE_CCP_XML, "upsertByBindingId", "lastEventTime");
    }

    @Test
    void admissionLogInsert_CheckedAt_ShouldUseUtcTypeHandlerOnParameter() throws Exception {
        assertParameterTypeHandler(ADMISSION_LOG_XML, "insertPo", "checkedAt");
    }

    // ---------- 审计单属主：写入口不再显式 now() ----------

    @Test
    void auditWriteStatements_ShouldNotWriteAuditTimeViaNow() throws Exception {
        // 覆盖全部 BaseDao XML 写入口（业务 SQL 亦不得 now() 写审计列）
        String[] mappers = {
                "/mappers/TboxMapper.xml",
                "/mappers/CcpMapper.xml",
                "/mappers/VehicleTboxMapper.xml",
                "/mappers/VehicleCcpMapper.xml",
                "/mappers/VehicleSimMapper.xml",
                "/mappers/CmdRecordMapper.xml",
                "/mappers/TboxLogMapper.xml",
                "/mappers/CcpLogMapper.xml",
                "/mappers/VehicleTboxLogMapper.xml",
                "/mappers/VehicleCcpLogMapper.xml",
                "/mappers/VehicleSimLogMapper.xml",
                "/mappers/DeviceAdmissionLogMapper.xml"
        };
        for (String mapperXml : mappers) {
            String content = readResource(mapperXml);
            assertFalse(content.contains("now()"),
                    mapperXml + " 仍包含显式 now() 写入（审计单属主要求 DB DEFAULT/ON UPDATE 唯一生成）");
        }
    }

    @Test
    void auditWriteStatements_ShouldDeclareDefaultAuditColumnsOnly() throws Exception {
        // insert 列清单应省略 create_time/modify_time（由 DB DEFAULT 生成）
        for (String mapperXml : new String[]{VEHICLE_TBOX_XML, VEHICLE_CCP_XML, ADMISSION_LOG_XML}) {
            String insertSql = getMapperSql(mapperXml, "insertPo");
            assertFalse(insertSql.contains("create_time"), mapperXml + " insertPo 仍显式写 create_time");
            assertFalse(insertSql.contains("modify_time"), mapperXml + " insertPo 仍显式写 modify_time");
        }
    }

    // ---------- 工具 ----------

    private void parse(Configuration configuration, String mapperXml) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream(mapperXml)) {
            assertNotNull(inputStream, "未找到 " + mapperXml);
            XMLMapperBuilder builder = new XMLMapperBuilder(
                    inputStream, configuration, mapperXml, configuration.getSqlFragments());
            builder.parse();
        }
    }

    private org.apache.ibatis.mapping.BoundSql getBoundSql(String mapperXml, String statementId) throws Exception {
        Configuration configuration = new Configuration();
        parse(configuration, mapperXml);
        org.apache.ibatis.mapping.MappedStatement statement = configuration.getMappedStatements().stream()
                .filter(ms -> ms.getId().endsWith("." + statementId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少语句: " + statementId));
        return statement.getSqlSource().getBoundSql(new java.util.HashMap<>());
    }

    private String getMapperSql(String mapperXml, String statementId) throws Exception {
        return getBoundSql(mapperXml, statementId).getSql();
    }

    private void assertParameterTypeHandler(String mapperXml, String statementId, String property) throws Exception {
        org.apache.ibatis.mapping.BoundSql boundSql = getBoundSql(mapperXml, statementId);
        org.apache.ibatis.mapping.ParameterMapping mapping = boundSql.getParameterMappings().stream()
                .filter(pm -> property.equals(pm.getProperty()))
                .findFirst()
                .orElse(null);
        assertNotNull(mapping, "缺少参数 property=" + property + "（statement=" + statementId + "）");
        assertTrue(mapping.getTypeHandler() instanceof UtcInstantTypeHandler,
                property + " 参数未使用 UtcInstantTypeHandler，实际=" + mapping.getTypeHandler());
    }

    private String readResource(String resource) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream(resource)) {
            assertNotNull(inputStream, "未找到 " + resource);
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private void assertSqlContains(String sql, String expectedFragment) {
        assertNotNull(sql);
        assertTrue(sql.contains(expectedFragment),
                "SQL 未包含 " + expectedFragment + "，实际=" + sql);
    }
}
