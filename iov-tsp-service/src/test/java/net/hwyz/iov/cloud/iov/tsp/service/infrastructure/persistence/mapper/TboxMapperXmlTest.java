package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mapper 映射集成测试
 * <p>
 * 校验 TSP-DSN-CR-008 相关语句已正确声明且可被 MyBatis 解析加载（不依赖数据库连接）：
 * <ul>
 *   <li>TboxMapper.xml：recordLoginById / completeActivationById / selectAllByHsm</li>
 *   <li>VehicleTboxMapper.xml：selectActiveBySnAndCategory（注册激活校验唯一 active 绑定）</li>
 *   <li>TboxRegistrationEventMapper：注解 SQL（消费幂等记录）</li>
 * </ul>
 */
class TboxMapperXmlTest {

    private static final String TBOX_MAPPER_XML = "/mappers/TboxMapper.xml";
    private static final String TBOX_NAMESPACE = "net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxMapper";
    private static final String VEHICLE_TBOX_MAPPER_XML = "/mappers/VehicleTboxMapper.xml";
    private static final String VEHICLE_TBOX_NAMESPACE = "net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleTboxMapper";
    private static final String TBOX_MGMT_MAPPER_XML = "/mappers/TboxManagementQueryMapper.xml";
    private static final String TBOX_MGMT_NAMESPACE = "net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxManagementQueryMapper";

    @Test
    void tboxMapperXml_ShouldLoadAndDeclareRegistrationStatements() throws Exception {
        Configuration configuration = new Configuration();
        parse(configuration, TBOX_MAPPER_XML);

        assertTrue(configuration.hasStatement(TBOX_NAMESPACE + ".recordLoginById"),
                "缺少 recordLoginById 语句");
        assertTrue(configuration.hasStatement(TBOX_NAMESPACE + ".completeActivationById"),
                "缺少 completeActivationById 语句");
        assertTrue(configuration.hasStatement(TBOX_NAMESPACE + ".selectByHsm"),
                "缺少 selectByHsm 语句");
        assertTrue(configuration.hasStatement(TBOX_NAMESPACE + ".selectAllByHsm"),
                "缺少 selectAllByHsm 语句");
    }

    @Test
    void vehicleTboxMapperXml_ShouldDeclareActiveBySnStatement() throws Exception {
        Configuration configuration = new Configuration();
        parse(configuration, VEHICLE_TBOX_MAPPER_XML);

        assertTrue(configuration.hasStatement(VEHICLE_TBOX_NAMESPACE + ".selectActiveBySnAndCategory"),
                "缺少 selectActiveBySnAndCategory 语句");
    }

    @Test
    void registrationEventMapper_ShouldRegisterAnnotatedStatements() {
        Configuration configuration = new Configuration();
        configuration.addMapper(TboxRegistrationEventMapper.class);

        String namespace = TboxRegistrationEventMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".selectByEventId"),
                "缺少 selectByEventId 语句");
        assertTrue(configuration.hasStatement(namespace + ".selectByEventTypeAndRequestId"),
                "缺少 selectByEventTypeAndRequestId 语句");
        assertTrue(configuration.hasStatement(namespace + ".selectByDeviceSn"),
                "缺少 selectByDeviceSn 语句");
        assertTrue(configuration.hasStatement(namespace + ".insert"),
                "缺少 insert 语句");
    }

    @Test
    void tboxManagementQueryMapperXml_ShouldDeclareListAndDetailStatements() throws Exception {
        Configuration configuration = new Configuration();
        parse(configuration, TBOX_MGMT_MAPPER_XML);

        assertTrue(configuration.hasStatement(TBOX_MGMT_NAMESPACE + ".selectPageByCriteria"),
                "缺少 selectPageByCriteria 语句");
        assertTrue(configuration.hasStatement(TBOX_MGMT_NAMESPACE + ".selectBySn"),
                "缺少 selectBySn 语句");
    }

    private void parse(Configuration configuration, String mapperXml) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream(mapperXml)) {
            assertNotNull(inputStream, "未找到 " + mapperXml);
            XMLMapperBuilder builder = new XMLMapperBuilder(
                    inputStream, configuration, mapperXml, configuration.getSqlFragments());
            builder.parse();
        }
    }
}
