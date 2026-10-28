package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JacksonInstantCompatConfig 模块集成测试（TSP-DSN-CR-012 加固）。
 * <p>
 * 复现并锁定 wire 契约：注册 {@link JacksonInstantCompatModule} 后，
 * <ul>
 *   <li>序列化 {@code Instant} 必须为 ISO-8601 字符串（JavaTimeModule 序列化器必须保留）；</li>
 *   <li>反序列化数字必须按 epoch 毫秒（自定义反序列化器覆盖 JSR310 默认的 epoch 秒语义）；</li>
 *   <li>反序列化 ISO 字符串保持兼容。</li>
 * </ul>
 * 回归背景：曾用 {@code Jackson2ObjectMapperBuilder.modules(...)} 注册导致 well-known modules
 * 自动发现被关闭，JavaTimeModule 未注册，响应序列化 {@code Instant} 抛
 * {@code InvalidDefinitionException: Java 8 date/time type java.time.Instant not supported}。
 * 此处模拟 Spring Boot 标准 customizer（JavaTimeModule + 禁用时间戳数字）+ 本服务模块的组装顺序。
 */
class JacksonInstantCompatConfigTest {

    /** 与 Spring Boot 自动装配相同的组装顺序：well-known/标准模块先注册，本服务模块后注册。 */
    private ObjectMapper buildMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.modulesToInstall(new JavaTimeModule());
        builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        builder.modulesToInstall(new JacksonInstantCompatModule());
        return builder.build();
    }

    @Test
    void instant_shouldSerializeToIso8601String() throws Exception {
        ObjectMapper mapper = buildMapper();
        String json = mapper.writeValueAsString(Instant.parse("2026-10-28T12:00:00Z"));
        assertEquals("\"2026-10-28T12:00:00Z\"", json);
    }

    @Test
    void instant_epochMillisNumber_shouldParseAsMillis() throws Exception {
        ObjectMapper mapper = buildMapper();
        // 2026-10-27T14:57:34.340Z 的 epoch 毫秒（hutool JSONUtil 对 Instant 的默认输出）
        Instant parsed = mapper.readValue("{\"t\":1793113054340}", TestBox.class).t;
        assertEquals(Instant.parse("2026-10-27T14:57:34.340Z"), parsed);
    }

    @Test
    void instant_isoString_shouldParse() throws Exception {
        ObjectMapper mapper = buildMapper();
        Instant parsed = mapper.readValue("{\"t\":\"2026-10-27T14:57:34.340Z\"}", TestBox.class).t;
        assertEquals(Instant.parse("2026-10-27T14:57:34.340Z"), parsed);
    }

    /** 测试包装：t 字段类型为 Instant */
    static class TestBox {
        public Instant t;
    }
}
