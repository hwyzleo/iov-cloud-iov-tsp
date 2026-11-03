package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.web.autoconfigure.IovJacksonAutoConfiguration;
import net.hwyz.iov.cloud.framework.web.properties.IovRequestTimeZoneProperties;
import net.hwyz.iov.cloud.framework.web.timezone.AuthenticatedUserTimeZoneResolver;
import net.hwyz.iov.cloud.framework.web.timezone.DisplayTimeFacade;
import net.hwyz.iov.cloud.framework.web.timezone.RequestTimeZoneFallbackResolver;
import net.hwyz.iov.cloud.framework.web.timezone.RequestTimeZoneInterceptor;
import net.hwyz.iov.cloud.framework.web.timezone.RequestTimeZoneSnapshotResolver;
import net.hwyz.iov.cloud.iov.tsp.api.vo.TboxVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptTboxSummaryVo;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.mpt.MptTboxController;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptTboxSummaryResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.MptTboxQueryAppService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TSP 真实装配 JSON java.time 时间契约测试（TSP-DSN-CR-013 兼容阶段 + TSP-DSN-CR-015 iso/展示时区组）。
 * <p>
 * 与生产一致：加载 Spring Boot {@link JacksonAutoConfiguration} + FW-WEB
 * {@link IovJacksonAutoConfiguration}（iov.web.jackson.date-format + zone-id=UTC（TSP-DSN-CR-014 联动）），
 * 注入实际生效的 Spring ObjectMapper，分两组验证：
 * <ol>
 *   <li><b>epoch-millis 回归组（CR-013 兼容阶段）</b>：MPT /list 五字段 epoch 毫秒数字（修复“数组”输出）、
 *       双读、启动探针、无关 customizer 共存（不再互相遮蔽）、Service/Feign VO 契约；</li>
 *   <li><b>iso 装配组（CR-015）</b>：date-format=iso 时 LocalDateTime 输出无偏移 ISO 展示墙钟字符串，
 *       MPT /list 经请求时区（X-Timezone &gt; 默认 Asia/Shanghai）投影：上海 +8、UTC 保持、无头默认上海。</li>
 * </ol>
 * ISO 输出（FR-013-1）为切换阶段验收目标，本阶段不启用（TSP-DSN-CR-013 §6）；CR-015 对 MPT 投影字段作局部例外。
 */
class TspJacksonTimeContractTest {

    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final LocalDateTime SAMPLE_LOCAL = LocalDateTime.of(2026, 9, 2, 8, 0, 0);
    private static final Instant SAMPLE_INSTANT = Instant.parse("2026-09-02T00:00:00Z");

    private ApplicationContextRunner runner(String dateFormat) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, IovJacksonAutoConfiguration.class))
                .withPropertyValues("iov.web.jackson.date-format=" + dateFormat, "iov.web.jackson.zone-id=UTC");
    }

    private ObjectMapper productionMapper(String dateFormat) {
        ObjectMapper[] holder = new ObjectMapper[1];
        runner(dateFormat).run(context -> holder[0] = context.getBean(ObjectMapper.class));
        return holder[0];
    }

    private ObjectMapper epochMapper() {
        return productionMapper("epoch-millis");
    }

    private ObjectMapper isoMapper() {
        return productionMapper("iso");
    }

    /**
     * 构造 MPT 控制器 + 真实框架时区解析链（拦截器绑定快照 → DisplayTimeFacade 读取）的 MockMvc。
     */
    private MockMvc mptMockMvc(MptTboxQueryAppService appService, ObjectMapper mapper) {
        IovRequestTimeZoneProperties tzProperties = new IovRequestTimeZoneProperties();
        ObjectProvider<AuthenticatedUserTimeZoneResolver> userProvider = mock(ObjectProvider.class);
        ObjectProvider<List<RequestTimeZoneFallbackResolver>> fallbackProvider = mock(ObjectProvider.class);
        RequestTimeZoneSnapshotResolver snapshotResolver =
                new RequestTimeZoneSnapshotResolver(tzProperties, userProvider, fallbackProvider);
        RequestTimeZoneInterceptor interceptor = new RequestTimeZoneInterceptor(snapshotResolver);
        DisplayTimeFacade facade = new DisplayTimeFacade(tzProperties, fallbackProvider);
        MptTboxController controller = new MptTboxController(appService, facade);
        return MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(interceptor)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    // ---------- 启动探针：两种 date-format + 显式 zone 均可启动 ----------

    @Test
    void 兼容阶段与iso配置_启动探针均通过() {
        runner("epoch-millis").run(context -> assertFalse(context.getStartupFailure() != null));
        runner("iso").run(context -> assertFalse(context.getStartupFailure() != null));
    }

    // ---------- epoch-millis 回归组（CR-013 兼容阶段） ----------

    @Test
    void epoch_mptList五时间字段_输出epoch毫秒数字_无数组无字符串() {
        ObjectMapper mapper = epochMapper();
        MptTboxSummaryVo vo = MptTboxSummaryVo.builder()
                .sn("SN-1")
                .firstLoginTime(SAMPLE_LOCAL)
                .lastLoginTime(SAMPLE_LOCAL.plusDays(8))
                .activateTime(SAMPLE_LOCAL.plusDays(30))
                .createdAt(SAMPLE_LOCAL)
                .updatedAt(SAMPLE_LOCAL.plusDays(1))
                .build();

        JsonNode node = mapper.valueToTree(vo);
        assertEpochMillis(node.get("firstLoginTime"), SAMPLE_LOCAL);
        assertEpochMillis(node.get("lastLoginTime"), SAMPLE_LOCAL.plusDays(8));
        assertEpochMillis(node.get("activateTime"), SAMPLE_LOCAL.plusDays(30));
        assertEpochMillis(node.get("createdAt"), SAMPLE_LOCAL);
        assertEpochMillis(node.get("updatedAt"), SAMPLE_LOCAL.plusDays(1));
        // 非空时不得是数组/字符串（回归：原 bug 输出数组）
        assertFalse(node.get("firstLoginTime").isArray());
        assertFalse(node.get("firstLoginTime").isTextual());
        assertTrue(node.get("firstLoginTime").isIntegralNumber());
    }

    @Test
    void epoch_mptList时间字段_null_保持null不为epoch0() {
        ObjectMapper mapper = epochMapper();
        MptTboxSummaryVo vo = MptTboxSummaryVo.builder().sn("SN-1").build();
        JsonNode node = mapper.valueToTree(vo);
        assertTrue(node.get("firstLoginTime").isNull());
        assertTrue(node.get("lastLoginTime").isNull());
        assertTrue(node.get("activateTime").isNull());
        assertTrue(node.get("createdAt").isNull());
        assertTrue(node.get("updatedAt").isNull());
    }

    @Test
    void epoch_httpList_五时间字段为epoch毫秒数字() throws Exception {
        ObjectMapper mapper = epochMapper();
        MptTboxQueryAppService appService = mock(MptTboxQueryAppService.class);
        when(appService.search(any(), any())).thenReturn(PageResult.<MptTboxSummaryResult>builder()
                .items(List.of(MptTboxSummaryResult.builder()
                        .sn("SN-1")
                        .firstLoginTime(SAMPLE_LOCAL)
                        .lastLoginTime(SAMPLE_LOCAL.plusDays(8))
                        .activateTime(SAMPLE_LOCAL.plusDays(30))
                        .createdAt(SAMPLE_LOCAL)
                        .updatedAt(SAMPLE_LOCAL.plusDays(1))
                        .build()))
                .total(1).page(1).size(20).build());
        MockMvc mockMvc = mptMockMvc(appService, mapper);

        // CR-015：MPT 展示投影按请求时区执行；UTC 头下投影保持恒等，五字段仍为 epoch 数字
        mockMvc.perform(get("/api/mpt/tbox/v1/list").header("X-Timezone", "UTC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].sn").value("SN-1"))
                .andExpect(jsonPath("$.data.items[0].firstLoginTime").isNumber())
                .andExpect(jsonPath("$.data.items[0].lastLoginTime").isNumber())
                .andExpect(jsonPath("$.data.items[0].activateTime").isNumber())
                .andExpect(jsonPath("$.data.items[0].createdAt").isNumber())
                .andExpect(jsonPath("$.data.items[0].updatedAt").isNumber());
    }

    // ---------- iso 装配组（TSP-DSN-CR-015：展示墙钟投影） ----------

    @Test
    void iso_mptList时间字段_输出无偏移ISO展示墙钟字符串() {
        ObjectMapper mapper = isoMapper();
        MptTboxSummaryVo vo = MptTboxSummaryVo.builder()
                .sn("SN-1")
                .firstLoginTime(SAMPLE_LOCAL)
                .lastLoginTime(SAMPLE_LOCAL.plusDays(8))
                .build();

        JsonNode node = mapper.valueToTree(vo);
        assertTrue(node.get("firstLoginTime").isTextual());
        assertEquals("2026-09-02T08:00:00", node.get("firstLoginTime").asText());
        assertEquals("2026-09-10T08:00:00", node.get("lastLoginTime").asText());
        // 无 offset 字符串（LocalDateTime 语义），不是数组/数字
        assertFalse(node.get("firstLoginTime").isArray());
        assertFalse(node.get("firstLoginTime").isIntegralNumber());
    }

    @Test
    void iso_mptList时间字段_null_保持null() {
        ObjectMapper mapper = isoMapper();
        MptTboxSummaryVo vo = MptTboxSummaryVo.builder().sn("SN-1").build();
        JsonNode node = mapper.valueToTree(vo);
        assertTrue(node.get("firstLoginTime").isNull());
        assertTrue(node.get("lastLoginTime").isNull());
    }

    @Test
    void iso_httpList_按请求时区输出展示墙钟() throws Exception {
        ObjectMapper mapper = isoMapper();
        MptTboxQueryAppService appService = mock(MptTboxQueryAppService.class);
        when(appService.search(any(), any())).thenReturn(PageResult.<MptTboxSummaryResult>builder()
                .items(List.of(MptTboxSummaryResult.builder()
                        .sn("SN-1")
                        .lastLoginTime(SAMPLE_LOCAL) // UTC 墙钟 2026-09-02T08:00
                        .build()))
                .total(1).page(1).size(20).build());
        MockMvc mockMvc = mptMockMvc(appService, mapper);

        // 上海头 → +8
        mockMvc.perform(get("/api/mpt/tbox/v1/list").header("X-Timezone", "Asia/Shanghai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].lastLoginTime").value("2026-09-02T16:00:00"));
        // UTC 头 → 保持
        mockMvc.perform(get("/api/mpt/tbox/v1/list").header("X-Timezone", "UTC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].lastLoginTime").value("2026-09-02T08:00:00"));
        // 无头 → 框架默认 Asia/Shanghai → +8
        mockMvc.perform(get("/api/mpt/tbox/v1/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].lastLoginTime").value("2026-09-02T16:00:00"));
    }

    // ---------- 双读：13 位毫秒 / 10 位秒 / ISO 同一时刻；非法格式拒绝 ----------

    @Test
    void 生产mapper双读_数字与ISO同一时刻() {
        ObjectMapper mapper = epochMapper();
        long millis = SAMPLE_INSTANT.toEpochMilli();
        long seconds = SAMPLE_INSTANT.getEpochSecond();
        assertEquals(SAMPLE_INSTANT, mapper.convertValue(millis, Instant.class));
        assertEquals(SAMPLE_INSTANT, mapper.convertValue(seconds, Instant.class));
        assertEquals(SAMPLE_INSTANT, mapper.convertValue(SAMPLE_INSTANT.toString(), Instant.class));
        // 同一时刻的数字与 ISO 归一相同（digest 一致性前提）
        assertEquals(mapper.convertValue(millis, Instant.class), mapper.convertValue(SAMPLE_INSTANT.toString(), Instant.class));
    }

    @Test
    void 生产mapper双读_非法格式拒绝() throws Exception {
        ObjectMapper mapper = epochMapper();
        // 数组、秒小数按框架规则拒绝（JsonMappingException，RD-001-3）
        org.junit.jupiter.api.Assertions.assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class,
                () -> mapper.readValue("[]", Instant.class));
        org.junit.jupiter.api.Assertions.assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class,
                () -> mapper.readValue("1793113054.5", Instant.class));
        // 无 offset 字符串：Instant.parse 直接抛 DateTimeParseException（框架 string 分支语义）
        org.junit.jupiter.api.Assertions.assertThrows(java.time.format.DateTimeParseException.class,
                () -> mapper.readValue("\"2026-09-02T08:00:00\"", Instant.class));
        // null → null（不转换为 epoch 0）
        assertNull(mapper.readValue("null", Instant.class));
    }

    // ---------- 无关 customizer 共存：框架策略不被遮蔽（原 bug 回归） ----------

    @Test
    void 无关customizer共存_框架策略生效() throws Exception {
        ObjectMapper[] holder = new ObjectMapper[1];
        runner("epoch-millis")
                .withUserConfiguration(UnrelatedCustomizerConfig.class)
                .run(context -> {
                    assertFalse(context.getStartupFailure() != null);
                    holder[0] = context.getBean(ObjectMapper.class);
                });
        ObjectMapper mapper = holder[0];
        // 框架策略仍生效：Instant 输出 epoch 毫秒数字
        JsonNode node = mapper.valueToTree(java.util.Map.of("instant", SAMPLE_INSTANT));
        assertTrue(node.get("instant").isIntegralNumber());
        assertEquals(SAMPLE_INSTANT.toEpochMilli(), node.get("instant").longValue());
        // 服务定制保留：自定义 String 序列化器
        assertEquals("\"SVC:hello\"", mapper.writeValueAsString("hello"));
    }

    @org.springframework.context.annotation.Configuration
    static class UnrelatedCustomizerConfig {
        @org.springframework.context.annotation.Bean
        public Jackson2ObjectMapperBuilderCustomizer unrelatedCustomizer() {
            return builder -> builder.modulesToInstall(new SimpleModule("svc-string") {
                {
                    addSerializer(String.class, new com.fasterxml.jackson.databind.JsonSerializer<String>() {
                        @Override
                        public void serialize(String value,
                                com.fasterxml.jackson.core.JsonGenerator gen,
                                com.fasterxml.jackson.databind.SerializerProvider serializers) throws java.io.IOException {
                            gen.writeString("SVC:" + value);
                        }
                    });
                }
            });
        }
    }

    // ---------- Service/Feign VO 契约（兼容阶段：毫秒数字输出；CR-015 不对 Service 做展示转换） ----------

    @Test
    void serviceVo_TboxVo_时间字段epoch毫秒数字() {
        ObjectMapper mapper = epochMapper();
        TboxVo vo = new TboxVo();
        vo.setFirstLoginTime(SAMPLE_LOCAL);
        vo.setLastLoginTime(SAMPLE_LOCAL.plusDays(8));
        vo.setActivateTime(SAMPLE_LOCAL.plusDays(30));
        JsonNode node = mapper.valueToTree(vo);
        assertEpochMillis(node.get("firstLoginTime"), SAMPLE_LOCAL);
        assertEpochMillis(node.get("lastLoginTime"), SAMPLE_LOCAL.plusDays(8));
        assertEpochMillis(node.get("activateTime"), SAMPLE_LOCAL.plusDays(30));
    }

    // IOV-TSP-DSN-CR-016：旧安全 DTO（DeviceAdmission*/VehicleAccessIdentity*）已迁出删除，
    // 其 JSON 时间契约不再属于 TSP 对外契约；保留 TboxVo/MptTboxSummaryVo 时间契约。

    // ---------- 辅助 ----------

    private static void assertEpochMillis(JsonNode node, LocalDateTime local) {
        assertTrue(node != null && node.isIntegralNumber(),
                () -> "应为 epoch 毫秒数字，实际: " + node);
        assertEquals(local.atZone(UTC_ZONE).toInstant().toEpochMilli(), node.longValue());
    }
}
