package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.mobile;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.security.interceptor.HeaderInterceptor;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ActivationSessionState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingOperationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UserVehicleRelationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.BindingOperationVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ConfirmVehicleBindingVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ScanActivationSessionVo;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.support.TrustedCallerResolver;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.UserVehicleRelationResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.UserVehicleBindingAppService;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.UserVehicleBindingErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 手机端绑车入口测试（IOV-TSP-DSN-CR-017 TC017-05/TC017-09、IOV-TSP-DSN-CR-018 TC018-01～09）。
 * <p>
 * 使用真实框架 HeaderInterceptor 装配可信上下文，验证「账号只取认证上下文」、
 * 「客户端类型不匹配即拒绝」、「VIN 必填单值且非法格式不查库」以及旧 /api/c 路由不再注册。
 */
class MobileActivationControllerTest {

    private static final String VIN = "LSVAA1234567890AB";

    private UserVehicleBindingAppService appService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        appService = mock(UserVehicleBindingAppService.class);
        TspUserVehicleProperties properties = new TspUserVehicleProperties();
        MobileActivationController controller =
                new MobileActivationController(appService, new TrustedCallerResolver(properties));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new HeaderInterceptor())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.remove();
    }

    @Test
    @DisplayName("扫码：账号取自可信认证上下文，返回最小车辆信息")
    void scanUsesTrustedAccount() throws Exception {
        when(appService.scan(eq("account-1"), eq("qr-token-1"))).thenReturn(ScanActivationSessionVo.builder()
                .sessionId("SES-1").vin(VIN).state(ActivationSessionState.SCANNED)
                .claimed(true).expiresAt(Instant.parse("2026-11-20T10:10:00Z")).build());

        mockMvc.perform(post("/api/mobile/activation/v1/session/scan")
                        .header("X-User-Id", "account-1")
                        .header("X-Client-Type", "MOBILE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\":\"qr-token-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value("SES-1"))
                .andExpect(jsonPath("$.data.vin").value(VIN));

        verify(appService).scan("account-1", "qr-token-1");
    }

    @Test
    @DisplayName("扫码：缺少可信账号上下文即拒绝")
    void scanRejectsMissingAccount() {
        assertThatThrownBy(() -> mockMvc.perform(post("/api/mobile/activation/v1/session/scan")
                        .header("X-Client-Type", "MOBILE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\":\"qr-token-1\"}")))
                .hasRootCauseInstanceOf(BusinessException.class)
                .rootCause()
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.INVALID_REQUEST.getCode()));
    }

    @Test
    @DisplayName("扫码：车机客户端类型不得调用手机端入口")
    void scanRejectsVehicleClientType() {
        assertThatThrownBy(() -> mockMvc.perform(post("/api/mobile/activation/v1/session/scan")
                        .header("X-User-Id", "account-1")
                        .header("X-Client-Type", "IVI")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\":\"qr-token-1\"}")))
                .hasRootCauseInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("取消会话：账号取自可信认证上下文")
    void cancelUsesTrustedAccount() throws Exception {
        mockMvc.perform(post("/api/mobile/activation/v1/session/cancel")
                        .header("X-User-Id", "account-1")
                        .header("X-Client-Type", "MOBILE")
                        .param("sessionId", "SES-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(appService).cancel("account-1", "SES-1");
    }

    @Test
    @DisplayName("确认绑定：透传媒等键与会话，返回幂等结果")
    void confirmPassesIdempotencyKey() throws Exception {
        when(appService.confirm(eq("account-1"), any())).thenReturn(ConfirmVehicleBindingVo.builder()
                .operationId("BOP-1").status(BindingOperationStatus.SUCCEEDED).relationId("REL-1")
                .vin(VIN).build());

        mockMvc.perform(post("/api/mobile/activation/v1/binding/confirm")
                        .header("X-User-Id", "account-1")
                        .header("X-Client-Type", "MOBILE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"SES-1\",\"idempotencyKey\":\"idem-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.relationId").value("REL-1"));
    }

    @Test
    @DisplayName("查询操作：仅本人操作可查")
    void queryOperationScopedToAccount() throws Exception {
        when(appService.queryOperation("account-1", "BOP-1")).thenReturn(BindingOperationVo.builder()
                .operationId("BOP-1").status(BindingOperationStatus.SUCCEEDED).build());

        mockMvc.perform(get("/api/mobile/activation/v1/binding/operations/BOP-1")
                        .header("X-User-Id", "account-1")
                        .header("X-Client-Type", "MOBILE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operationId").value("BOP-1"));
    }

    @Test
    @DisplayName("当前关系：存在有效关系返回 BOUND 且不暴露内部字段")
    void currentRelationReturnsBound() throws Exception {
        when(appService.queryCurrentRelation("account-1", VIN)).thenReturn(UserVehicleRelationResult.builder()
                .status(UserVehicleRelationStatus.BOUND).vin(VIN).relationId("REL-1").role("PRIMARY_USER")
                .boundAt(Instant.parse("2026-11-21T10:00:00Z")).build());

        mockMvc.perform(get("/api/mobile/activation/v1/binding")
                        .header("X-User-Id", "account-1")
                        .header("X-Client-Type", "MOBILE")
                        .param("vin", VIN.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BOUND"))
                .andExpect(jsonPath("$.data.vin").value(VIN))
                .andExpect(jsonPath("$.data.relationId").value("REL-1"))
                .andExpect(jsonPath("$.data.role").value("PRIMARY_USER"))
                .andExpect(jsonPath("$.data.boundAt").exists())
                .andExpect(jsonPath("$.data.accountId").doesNotExist())
                .andExpect(jsonPath("$.data.state").doesNotExist())
                .andExpect(jsonPath("$.data.factRef").doesNotExist())
                .andExpect(jsonPath("$.data.sourceOperationId").doesNotExist());

        // VIN 规范化后查询，且请求参数中的 accountId 不得作为权威
        verify(appService).queryCurrentRelation("account-1", VIN);
    }

    @Test
    @DisplayName("当前关系：无有效关系返回成功 + NOT_BOUND，不返回他人信息")
    void currentRelationReturnsNotBound() throws Exception {
        when(appService.queryCurrentRelation("account-1", VIN)).thenReturn(UserVehicleRelationResult.builder()
                .status(UserVehicleRelationStatus.NOT_BOUND).vin(VIN).build());

        mockMvc.perform(get("/api/mobile/activation/v1/binding")
                        .header("X-User-Id", "account-1")
                        .header("X-Client-Type", "MOBILE")
                        .param("vin", VIN)
                        .param("accountId", "other-account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("NOT_BOUND"))
                .andExpect(jsonPath("$.data.vin").value(VIN))
                .andExpect(jsonPath("$.data.relationId").doesNotExist());

        verify(appService).queryCurrentRelation("account-1", VIN);
    }

    @Test
    @DisplayName("当前关系：缺少 VIN 按参数错误拒绝且不查询数据库")
    void currentRelationRejectsMissingVin() {
        assertInvalidVin(get("/api/mobile/activation/v1/binding")
                .header("X-User-Id", "account-1")
                .header("X-Client-Type", "MOBILE"));
    }

    @Test
    @DisplayName("当前关系：空白 VIN 按参数错误拒绝且不查询数据库")
    void currentRelationRejectsBlankVin() {
        assertInvalidVin(get("/api/mobile/activation/v1/binding")
                .header("X-User-Id", "account-1")
                .header("X-Client-Type", "MOBILE")
                .param("vin", "  "));
    }

    @Test
    @DisplayName("当前关系：非法格式 VIN 按参数错误拒绝且不查询数据库")
    void currentRelationRejectsInvalidVinFormat() {
        // 含非法字符 I，且长度不足
        assertInvalidVin(get("/api/mobile/activation/v1/binding")
                .header("X-User-Id", "account-1")
                .header("X-Client-Type", "MOBILE")
                .param("vin", "LSVIA123"));
    }

    @Test
    @DisplayName("当前关系：多值 VIN 按参数错误拒绝（只接受单值）")
    void currentRelationRejectsMultipleVinValues() {
        assertInvalidVin(get("/api/mobile/activation/v1/binding")
                .header("X-User-Id", "account-1")
                .header("X-Client-Type", "MOBILE")
                .param("vin", VIN, "LSVAA1234567890AC"));
    }

    @Test
    @DisplayName("当前关系：未认证不得查询")
    void currentRelationRejectsMissingAccount() {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/mobile/activation/v1/binding")
                        .header("X-Client-Type", "MOBILE")
                        .param("vin", VIN)))
                .hasRootCauseInstanceOf(BusinessException.class)
                .rootCause()
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.INVALID_REQUEST.getCode()));
        verifyNoInteractions(appService);
    }

    @Test
    @DisplayName("当前关系：车机客户端类型不得调用手机端入口")
    void currentRelationRejectsVehicleClientType() {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/mobile/activation/v1/binding")
                        .header("X-User-Id", "account-1")
                        .header("X-Client-Type", "IVI")
                        .param("vin", VIN)))
                .hasRootCauseInstanceOf(BusinessException.class);
        verifyNoInteractions(appService);
    }

    @Test
    @DisplayName("旧 /api/c 手机端路由不再注册")
    void legacyCRoutesAreNotRegistered() throws Exception {
        mockMvc.perform(post("/api/c/activation/v1/session/scan")
                        .header("X-User-Id", "account-1")
                        .header("X-Client-Type", "MOBILE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\":\"qr-token-1\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/c/activation/v1/session/cancel")
                        .header("X-User-Id", "account-1")
                        .header("X-Client-Type", "MOBILE")
                        .param("sessionId", "SES-1"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/c/activation/v1/binding/confirm")
                        .header("X-User-Id", "account-1")
                        .header("X-Client-Type", "MOBILE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"SES-1\",\"idempotencyKey\":\"idem-1\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/c/activation/v1/binding/operations/BOP-1")
                        .header("X-User-Id", "account-1")
                        .header("X-Client-Type", "MOBILE"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/c/activation/v1/binding")
                        .header("X-User-Id", "account-1")
                        .header("X-Client-Type", "MOBILE")
                        .param("vin", VIN))
                .andExpect(status().isNotFound());
        verifyNoInteractions(appService);
    }

    private void assertInvalidVin(org.springframework.test.web.servlet.RequestBuilder request) {
        assertThatThrownBy(() -> mockMvc.perform(request))
                .hasRootCauseInstanceOf(BusinessException.class)
                .rootCause()
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.INVALID_REQUEST.getCode()));
        verifyNoInteractions(appService);
    }
}
