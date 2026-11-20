package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.c;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.security.interceptor.HeaderInterceptor;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ActivationSessionState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingOperationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.BindingOperationVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ConfirmVehicleBindingVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ScanActivationSessionVo;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.support.TrustedCallerResolver;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C 端绑车入口测试（IOV-TSP-DSN-CR-017 TC017-05/TC017-09）。
 * <p>
 * 使用真实框架 HeaderInterceptor 装配可信上下文，验证「账号只取认证上下文」与
 * 「客户端类型不匹配即拒绝」，不新造自定义身份头。
 */
class CActivationControllerTest {

    private UserVehicleBindingAppService appService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        appService = mock(UserVehicleBindingAppService.class);
        TspUserVehicleProperties properties = new TspUserVehicleProperties();
        CActivationController controller = new CActivationController(appService, new TrustedCallerResolver(properties));
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
                .sessionId("SES-1").vin("LSVAA1234567890AB").state(ActivationSessionState.SCANNED)
                .claimed(true).expiresAt(Instant.parse("2026-11-20T10:10:00Z")).build());

        mockMvc.perform(post("/api/c/activation/v1/session/scan")
                        .header("X-User-Id", "account-1")
                        .header("X-Client-Type", "MOBILE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\":\"qr-token-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value("SES-1"))
                .andExpect(jsonPath("$.data.vin").value("LSVAA1234567890AB"));

        verify(appService).scan("account-1", "qr-token-1");
    }

    @Test
    @DisplayName("扫码：缺少可信账号上下文即拒绝")
    void scanRejectsMissingAccount() {
        assertThatThrownBy(() -> mockMvc.perform(post("/api/c/activation/v1/session/scan")
                        .header("X-Client-Type", "MOBILE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\":\"qr-token-1\"}")))
                .hasRootCauseInstanceOf(BusinessException.class)
                .rootCause()
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.INVALID_REQUEST.getCode()));
    }

    @Test
    @DisplayName("扫码：车机客户端类型不得调用 C 端入口")
    void scanRejectsVehicleClientType() {
        assertThatThrownBy(() -> mockMvc.perform(post("/api/c/activation/v1/session/scan")
                        .header("X-User-Id", "account-1")
                        .header("X-Client-Type", "IVI")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\":\"qr-token-1\"}")))
                .hasRootCauseInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("确认绑定：透传媒等键与会话，返回幂等结果")
    void confirmPassesIdempotencyKey() throws Exception {
        when(appService.confirm(eq("account-1"), any())).thenReturn(ConfirmVehicleBindingVo.builder()
                .operationId("BOP-1").status(BindingOperationStatus.SUCCEEDED).relationId("REL-1")
                .vin("LSVAA1234567890AB").build());

        mockMvc.perform(post("/api/c/activation/v1/binding/confirm")
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

        mockMvc.perform(get("/api/c/activation/v1/binding/operations/BOP-1")
                        .header("X-User-Id", "account-1")
                        .header("X-Client-Type", "MOBILE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operationId").value("BOP-1"));
    }
}
