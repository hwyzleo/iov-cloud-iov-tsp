package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全量 Mapper XML 解析健全性测试（IOV-TSP-DSN-CR-016 补强）
 * <p>
 * 校验 src/main/resources/mappers 下全部 XML 均可被 MyBatis 解析、namespace 对应接口类存在，
 * 防止删除实现类时漏删 XML 导致 Spring 启动期 sqlSessionFactory 解析失败
 * （如旧 DeviceAdmissionLogMapper.xml 残留引用已删 PO）。
 * </p>
 */
class MapperXmlSanityTest {

    @Test
    void allMapperXml_ShouldParse_AndResolveInterface() throws Exception {
        Path mappersDir = Paths.get("src/main/resources/mappers");
        assertTrue(Files.isDirectory(mappersDir), "mappers 目录不存在: " + mappersDir);

        try (var files = Files.list(mappersDir)) {
            files.filter(p -> p.toString().endsWith(".xml")).forEach(xml -> {
                String resourcePath = "/mappers/" + xml.getFileName();
                try (InputStream in = MapperXmlSanityTest.class.getResourceAsStream(resourcePath)) {
                    assertNotNull(in, "缺少资源: " + resourcePath);
                    Configuration configuration = new Configuration();
                    new XMLMapperBuilder(in, configuration, resourcePath, configuration.getSqlFragments()).parse();
                    // namespace 对应接口类必须存在（防止漏删 XML）
                    String ns = extractNamespace(xml);
                    assertNotNull(ns, xml.getFileName() + " 缺少 namespace");
                    Class.forName(ns);
                } catch (Exception e) {
                    throw new IllegalStateException("XML 解析失败: " + xml.getFileName(), e);
                }
            });
        }
    }

    private String extractNamespace(Path xml) throws Exception {
        String content = Files.readString(xml);
        int start = content.indexOf("namespace=\"");
        if (start < 0) {
            return null;
        }
        int from = start + "namespace=\"".length();
        int to = content.indexOf('"', from);
        return content.substring(from, to);
    }
}
