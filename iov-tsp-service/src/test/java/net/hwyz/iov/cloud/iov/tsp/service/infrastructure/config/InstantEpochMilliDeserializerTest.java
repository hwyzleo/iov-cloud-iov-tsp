package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * InstantEpochMilliDeserializer 单元测试（TSP-DSN-CR-012 加固）。
 * <p>
 * 锁定 wire 契约：ISO-8601 字符串按 ISO 解析；数字一律按 epoch 毫秒解析
 * （覆盖 Jackson/JSR310 "数字按 epoch 秒"的默认，防止旧版 VMD 消息被误读为未来时间）。
 *
 * @author hwyz_leo
 * @since 2026-10-27
 */
class InstantEpochMilliDeserializerTest {

    private static final Instant ISO_INSTANT = Instant.parse("2026-10-27T14:57:34.340Z");
    /** 2026-10-27T14:57:34.340Z 的 epoch 毫秒（hutool JSONUtil 对 Instant 的默认输出） */
    private static final long EPOCH_MILLIS = 1793113054340L;

    /**
     * 模拟生产注册顺序：JavaTimeModule 先注册（service-loader/Spring Boot），
     * 兼容模块后注册并覆盖同一类型的反序列化器。
     */
    private ObjectMapper productionLikeMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JacksonInstantCompatModule());
    }

    @Test
    void deserialize_IsoString_ShouldParse() throws Exception {
        String json = "{\"t\":\"2026-10-27T14:57:34.340Z\"}";
        Wrapper wrapper = productionLikeMapper()
                .readerFor(Wrapper.class)
                .readValue(json);
        assertEquals(ISO_INSTANT, wrapper.t);
    }

    @Test
    void deserialize_EpochMillisNumber_ShouldParseAsMillis() throws Exception {
        // 旧版 VMD（hutool）真实 wire 格式：数字 = epoch 毫秒。
        // Jackson 默认按 epoch 秒会读成 +58791-06-29，加固后必须还原为 2026-10-27。
        String json = "{\"t\":" + EPOCH_MILLIS + "}";
        Wrapper wrapper = productionLikeMapper()
                .readerFor(Wrapper.class)
                .readValue(json);
        assertEquals(ISO_INSTANT, wrapper.t);
    }

    @Test
    void deserialize_IsoStringAndEpochMillis_ShouldBeEquivalent() throws Exception {
        Wrapper fromIso = productionLikeMapper().readerFor(Wrapper.class)
                .readValue("{\"t\":\"2026-10-27T14:57:34.340Z\"}");
        Wrapper fromMillis = productionLikeMapper().readerFor(Wrapper.class)
                .readValue("{\"t\":" + EPOCH_MILLIS + "}");
        assertEquals(fromIso.t, fromMillis.t);
    }

    @Test
    void deserialize_MalformedIsoString_ShouldThrow() {
        // 字符串必须可被 Instant.parse 解析；非法字符串 fail-closed（契约错误路径）。
        // Jackson 会将 DateTimeParseException 包装为 JsonMappingException。
        JsonMappingException e = assertThrows(JsonMappingException.class, () -> productionLikeMapper()
                .readerFor(Wrapper.class)
                .readValue("{\"t\":\"not-a-time\"}"));
        assertTrue(e.getCause() instanceof DateTimeParseException,
                "根因应为 DateTimeParseException，实际: " + e.getCause());
    }

    @Test
    void deserialize_Array_ShouldThrowMappingError() {
        assertThrows(JsonMappingException.class, () -> productionLikeMapper()
                .readerFor(Wrapper.class)
                .readValue("{\"t\":[]}"));
    }

    /** 测试包装：t 字段类型为 Instant */
    public static class Wrapper {
        public Instant t;
    }
}
