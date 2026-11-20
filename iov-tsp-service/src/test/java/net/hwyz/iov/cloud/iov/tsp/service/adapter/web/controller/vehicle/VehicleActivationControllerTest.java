package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.vehicle;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.security.interceptor.HeaderInterceptor;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.AgreementType;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.CreateActivationSessionVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ReportAckVo;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.support.TrustedCallerResolver;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.ConsentAppService;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.UserVehicleBindingAppService;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.UserVehicleBindingErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AgreementBundle;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AgreementContent;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 车端绑车入口测试（IOV-TSP-DSN-CR-017 TC017-05/TC017-09）。
 * <p>
 * 验证车辆身份只取可信车端上下文（X-Vin + 车机客户端类型），App 端不可调用车端入口。
 */
class VehicleActivationControllerTest {

    private static final String VIN = "LSVAA1234567890AB";

    private UserVehicleBindingAppService bindingAppService;
    private ConsentAppService consentAppService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        bindingAppService = mock(UserVehicleBindingAppService.class);
        consentAppService = mock(ConsentAppService.class);
        VehicleActivationController controller = new VehicleActivationController(bindingAppService,
                consentAppService, new TrustedCallerResolver(new TspUserVehicleProperties()));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new HeaderInterceptor())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.remove();
    }

    @Test
    @DisplayName("查询协议：车机上下文可读，返回两项协议修订")
    void agreementsRequireVehicleContext() throws Exception {
        when(bindingAppService.queryAgreements("zh-CN")).thenReturn(new AgreementBundle("B-1", "zh-CN",
                List.of(new AgreementContent("PRIVACY_POLICY", "1.0", "REV-P", "zh-CN", "隐私政策", "hash", "<p/>"),
                        new AgreementContent("USER_AGREEMENT", "1.0", "REV-U", "zh-CN", "用户协议", "hash", "<p/>"))));

        mockMvc.perform(get("/api/vehicle/activation/v1/agreements")
                        .header("X-Client-Type", "IVI")
                        .header("X-Vin", VIN)
                        .param("language", "zh-CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bundleId").value("B-1"))
                .andExpect(jsonPath("$.data.agreements[0].agreementType").value(AgreementType.PRIVACY_POLICY.name()));
    }

    @Test
    @DisplayName("查询协议：缺少 X-Vin 即拒绝（不使用请求体/普通头自报 VIN）")
    void agreementsRejectMissingTrustedVin() {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/vehicle/activation/v1/agreements")
                        .header("X-Client-Type", "IVI")))
                .hasRootCauseInstanceOf(BusinessException.class)
                .rootCause()
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.INVALID_REQUEST.getCode()));
    }

    @Test
    @DisplayName("查询协议：App 客户端类型不得调用车端入口")
    void agreementsRejectAppClientType() {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/vehicle/activation/v1/agreements")
                        .header("X-Client-Type", "MOBILE")
                        .header("X-Vin", VIN)))
                .hasRootCauseInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("申请会话：使用可信 VIN 与车端引用签发二维码")
    void createSessionUsesTrustedVin() throws Exception {
        when(bindingAppService.createSession(any(), eq(VIN), any())).thenReturn(CreateActivationSessionVo.builder()
                .sessionId("SES-1").qrToken("qr-token").expiresAt(Instant.parse("2026-11-20T10:10:00Z")).build());

        mockMvc.perform(post("/api/vehicle/activation/v1/session")
                        .header("X-Client-Type", "IVI")
                        .header("X-Vin", VIN)
                        .header("X-Device-Id", "device-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agreements\":[{\"agreementType\":\"PRIVACY_POLICY\",\"agreed\":true,"
                                + "\"version\":\"1.0\",\"revisionId\":\"REV-P\"},"
                                + "{\"agreementType\":\"USER_AGREEMENT\",\"agreed\":true,"
                                + "\"version\":\"1.0\",\"revisionId\":\"REV-U\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value("SES-1"));
    }

    @Test
    @DisplayName("跳过激活：只记录 VIN 级同意并返回回执")
    void skipReturnsAck() throws Exception {
        when(consentAppService.skipActivation(eq(VIN), any())).thenReturn(ReportAckVo.builder()
                .reportId("RPT-1").accepted(true).build());

        mockMvc.perform(post("/api/vehicle/activation/v1/session/skip")
                        .header("X-Client-Type", "IVI")
                        .header("X-Vin", VIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":\"RPT-1\",\"agreements\":[{\"agreementType\":\"PRIVACY_POLICY\","
                                + "\"agreed\":true,\"version\":\"1.0\",\"revisionId\":\"REV-P\"},"
                                + "{\"agreementType\":\"USER_AGREEMENT\",\"agreed\":true,"
                                + "\"version\":\"1.0\",\"revisionId\":\"REV-U\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true));
    }

    @Test
    @DisplayName("隐私授权补报：Account 取认证上下文，VIN 取车端上下文")
    void privacyReportUsesTrustedContext() throws Exception {
        when(consentAppService.reportPrivacyAuthorization(eq("account-1"), eq(VIN), any()))
                .thenReturn(ReportAckVo.builder().reportId("RPT-9").accepted(true).build());

        mockMvc.perform(post("/api/vehicle/activation/v1/privacy-authorization")
                        .header("X-Client-Type", "IVI")
                        .header("X-Vin", VIN)
                        .header("X-User-Id", "account-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":\"RPT-9\",\"items\":[{\"authorizationItem\":\"LOCATION\","
                                + "\"state\":\"GRANTED\",\"version\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true));

        org.mockito.ArgumentCaptor<net.hwyz.iov.cloud.iov.tsp.api.vo.request.ReportPrivacyAuthorizationRequest>
                captor = org.mockito.ArgumentCaptor.forClass(
                net.hwyz.iov.cloud.iov.tsp.api.vo.request.ReportPrivacyAuthorizationRequest.class);
        verify(consentAppService).reportPrivacyAuthorization(eq("account-1"), eq(VIN), captor.capture());
        assertThat(captor.getValue().getItems()).hasSize(1);
        assertThat(captor.getValue().getItems().get(0).getAuthorizationItem()).isEqualTo("LOCATION");
    }

    @Test
    @DisplayName("隐私授权补报：缺少登录账号上下文即拒绝")
    void privacyReportRejectsMissingAccount() {
        assertThatThrownBy(() -> mockMvc.perform(post("/api/vehicle/activation/v1/privacy-authorization")
                        .header("X-Client-Type", "IVI")
                        .header("X-Vin", VIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":\"RPT-9\",\"items\":[{\"authorizationItem\":\"LOCATION\","
                                + "\"state\":\"GRANTED\",\"version\":1}]}")))
                .hasRootCauseInstanceOf(BusinessException.class);
    }
}
