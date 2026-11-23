package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.vehicle;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.security.interceptor.HeaderInterceptor;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.AgreementType;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.CreateActivationSessionVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.LoginTicketVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ReportAckVo;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.support.TrustedCallerResolver;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.ActivationLoginTicketAppService;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.ConsentAppService;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.UserVehicleBindingAppService;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.UserVehicleBindingErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AgreementBundle;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AgreementContent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.PrivacyAuthorizationCatalog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.service.PrivacyAuthorizationCatalogPolicy;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 车端绑车入口测试（IOV-TSP-DSN-CR-017 TC017-05/TC017-09、IOV-TSP-DSN-CR-020 TC020-01/TC020-03）。
 * <p>
 * 验证车辆身份只取可信车端上下文（X-Vin + 车机客户端类型），App 端不可调用车端入口；
 * CR-020：授权项目录端点（Accept-Language / Cache-Control / 无 currentVersion）与会话幂等头。
 */
class VehicleActivationControllerTest {

    private static final String VIN = "LSVAA1234567890AB";

    private static final PrivacyAuthorizationCatalog CATALOG = PrivacyAuthorizationCatalogPolicy.build("v1",
            List.of(new PrivacyAuthorizationCatalogPolicy.RawItem("GPS_LOCATION", "位置信息",
                            "Location Information", false),
                    new PrivacyAuthorizationCatalogPolicy.RawItem("DRIVING_BEHAVIOR", "驾驶行为分析",
                            "Driving Behavior Analysis", false)));

    private UserVehicleBindingAppService bindingAppService;
    private ConsentAppService consentAppService;
    private ActivationLoginTicketAppService loginTicketAppService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        bindingAppService = mock(UserVehicleBindingAppService.class);
        consentAppService = mock(ConsentAppService.class);
        loginTicketAppService = mock(ActivationLoginTicketAppService.class);
        VehicleActivationController controller = new VehicleActivationController(bindingAppService,
                consentAppService, loginTicketAppService, CATALOG, new TspUserVehicleProperties(),
                new TrustedCallerResolver(new TspUserVehicleProperties()));
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
        when(bindingAppService.createSession(any(), eq(VIN), any(), any())).thenReturn(CreateActivationSessionVo.builder()
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
    @DisplayName("申请会话：Idempotency-Key 请求头透传到应用层（D020-05）")
    void createSessionPassesIdempotencyKey() throws Exception {
        when(bindingAppService.createSession(any(), eq(VIN), any(), eq("7c1f9a2e-session-1")))
                .thenReturn(CreateActivationSessionVo.builder().sessionId("SES-1").qrToken("qr-token")
                        .expiresAt(Instant.parse("2026-11-20T10:10:00Z")).build());

        mockMvc.perform(post("/api/vehicle/activation/v1/session")
                        .header("X-Client-Type", "IVI")
                        .header("X-Vin", VIN)
                        .header("X-Device-Id", "device-1")
                        .header("Idempotency-Key", "7c1f9a2e-session-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agreements\":[{\"agreementType\":\"PRIVACY_POLICY\",\"agreed\":true,"
                                + "\"version\":\"1.0\",\"revisionId\":\"REV-P\"},"
                                + "{\"agreementType\":\"USER_AGREEMENT\",\"agreed\":true,"
                                + "\"version\":\"1.0\",\"revisionId\":\"REV-U\"}]}"))
                .andExpect(status().isOk());

        verify(bindingAppService).createSession(any(), eq(VIN), any(), eq("7c1f9a2e-session-1"));
    }

    // ================= D020-02：车端只读授权项目录端点 =================

    @Test
    @DisplayName("目录端点：返回 catalogVersion 与条目（无 currentVersion）且带私有缓存头")
    void privacyCatalogReturnsItemsWithCacheControl() throws Exception {
        mockMvc.perform(get("/api/vehicle/activation/v1/privacy-authorization/items")
                        .header("X-Client-Type", "IVI")
                        .header("X-Vin", VIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "private, max-age=300"))
                // R-0：成功码统一 000000
                .andExpect(jsonPath("$.code").value("000000"))
                .andExpect(jsonPath("$.data.catalogVersion").value("v1"))
                .andExpect(jsonPath("$.data.items[0].authorizationItem").value("GPS_LOCATION"))
                .andExpect(jsonPath("$.data.items[0].displayName").value("位置信息"))
                .andExpect(jsonPath("$.data.items[0].required").value(false))
                .andExpect(jsonPath("$.data.items[0].currentVersion").doesNotExist());
    }

    @Test
    @DisplayName("目录端点：Accept-Language=en-US 返回英文展示名")
    void privacyCatalogLocalizesEnglish() throws Exception {
        mockMvc.perform(get("/api/vehicle/activation/v1/privacy-authorization/items")
                        .header("X-Client-Type", "IVI")
                        .header("X-Vin", VIN)
                        .header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].displayName").value("Location Information"));
    }

    @Test
    @DisplayName("目录端点：不支持的语言回落 zh-CN（不报 406、不报错）")
    void privacyCatalogFallsBackToDefaultLanguage() throws Exception {
        mockMvc.perform(get("/api/vehicle/activation/v1/privacy-authorization/items")
                        .header("X-Client-Type", "IVI")
                        .header("X-Vin", VIN)
                        .header("Accept-Language", "fr-FR,fr;q=0.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].displayName").value("位置信息"));
    }

    @Test
    @DisplayName("目录端点：缺少可信车端上下文即拒绝")
    void privacyCatalogRequiresVehicleContext() {
        assertThatThrownBy(() -> mockMvc.perform(
                        get("/api/vehicle/activation/v1/privacy-authorization/items")
                                .header("X-Client-Type", "MOBILE")
                                .header("X-Vin", VIN)))
                .hasRootCauseInstanceOf(BusinessException.class);
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
                        .content("{\"reportId\":\"RPT-9\",\"catalogVersion\":\"v1\","
                                + "\"items\":[{\"authorizationItem\":\"GPS_LOCATION\","
                                + "\"state\":\"GRANTED\",\"version\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true));

        org.mockito.ArgumentCaptor<net.hwyz.iov.cloud.iov.tsp.api.vo.request.ReportPrivacyAuthorizationRequest>
                captor = org.mockito.ArgumentCaptor.forClass(
                net.hwyz.iov.cloud.iov.tsp.api.vo.request.ReportPrivacyAuthorizationRequest.class);
        verify(consentAppService).reportPrivacyAuthorization(eq("account-1"), eq(VIN), captor.capture());
        assertThat(captor.getValue().getItems()).hasSize(1);
        assertThat(captor.getValue().getItems().get(0).getAuthorizationItem()).isEqualTo("GPS_LOCATION");
        assertThat(captor.getValue().getCatalogVersion()).isEqualTo("v1");
    }

    @Test
    @DisplayName("B-1：存量报文（无 catalogVersion）通过请求层校验，交服务层按开关决定接受/拒绝")
    void privacyReportWithoutCatalogVersionReachesService() throws Exception {
        when(consentAppService.reportPrivacyAuthorization(eq("account-1"), eq(VIN), any()))
                .thenReturn(ReportAckVo.builder().reportId("RPT-9").accepted(true).build());

        mockMvc.perform(post("/api/vehicle/activation/v1/privacy-authorization")
                        .header("X-Client-Type", "IVI")
                        .header("X-Vin", VIN)
                        .header("X-User-Id", "account-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":\"RPT-9\","
                                + "\"items\":[{\"authorizationItem\":\"GPS_LOCATION\","
                                + "\"state\":\"GRANTED\",\"version\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true));

        // catalogVersion 不再由 @NotBlank 拦在请求层（终审修复 B-1）：缺失时仍到达 Service
        verify(consentAppService).reportPrivacyAuthorization(eq("account-1"), eq(VIN),
                org.mockito.ArgumentMatchers.argThat(r -> r.getCatalogVersion() == null));
    }

    @Test
    @DisplayName("隐私授权补报：缺少登录账号上下文即拒绝")
    void privacyReportRejectsMissingAccount() {
        assertThatThrownBy(() -> mockMvc.perform(post("/api/vehicle/activation/v1/privacy-authorization")
                        .header("X-Client-Type", "IVI")
                        .header("X-Vin", VIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":\"RPT-9\",\"catalogVersion\":\"v1\","
                                + "\"items\":[{\"authorizationItem\":\"GPS_LOCATION\","
                                + "\"state\":\"GRANTED\",\"version\":1}]}")))
                .hasRootCauseInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("登录票据签发：车端上下文有效时返回票据原值与到期时刻")
    void issueLoginTicketReturnsTicket() throws Exception {
        Instant expiresAt = Instant.parse("2026-11-22T10:03:00Z");
        when(loginTicketAppService.issue(eq(VIN), eq("SES-1"))).thenReturn(LoginTicketVo.builder()
                .ticket("TICKET-VALUE").expiresAt(expiresAt).sessionId("SES-1").relationId("REL-1").build());

        mockMvc.perform(post("/api/vehicle/activation/v1/session/SES-1/login-ticket")
                        .header("X-Client-Type", "IVI")
                        .header("X-Vin", VIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket").value("TICKET-VALUE"))
                .andExpect(jsonPath("$.data.sessionId").value("SES-1"))
                .andExpect(jsonPath("$.data.relationId").value("REL-1"));

        verify(loginTicketAppService).issue(VIN, "SES-1");
    }

    @Test
    @DisplayName("登录票据签发：非车机客户端不可调用（防止手机端直接换票）")
    void issueLoginTicketRejectsAppClient() {
        assertThatThrownBy(() -> mockMvc.perform(post("/api/vehicle/activation/v1/session/SES-1/login-ticket")
                        .header("X-Client-Type", "MOBILE")
                        .header("X-Vin", VIN)))
                .hasRootCauseInstanceOf(BusinessException.class);
        verify(loginTicketAppService, org.mockito.Mockito.never()).issue(anyString(), anyString());
    }
}
