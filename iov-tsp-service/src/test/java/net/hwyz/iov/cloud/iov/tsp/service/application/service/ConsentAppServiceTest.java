package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ActivationSessionState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.AgreementType;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.IviLoginFailureReason;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.IviLoginResultStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.PrivacyAuthorizationState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.AgreementAcceptanceRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.ReportIviLoginResultRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.ReportPrivacyAuthorizationRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.SkipActivationRequest;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.UserVehicleBindingErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AccountAgreementConsent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationSession;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AgreementConsent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.PrivacyAuthorization;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.PrivacyAuthorizationCatalog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleRelation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationSessionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ConsentRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.PrivacyAuthorizationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleAuditRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleRelationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.service.PrivacyAuthorizationCatalogPolicy;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 同意与隐私授权测试（IOV-TSP-DSN-CR-017 TC017-06/TC017-07、IOV-TSP-DSN-CR-020 TC020-02/TC020-04）。
 * <p>
 * CR-020 覆盖：上报两阶段校验（401118/401119 整批拒绝、零写入）、决策序号三态收敛、
 * FAILED 缺号可记录、R-1 终态保护、R-2 审计仅首次。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsentAppServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");
    private static final String VIN = "LSVAA1234567890AB";
    private static final String ACCOUNT = "account-1";
    private static final String SESSION_ID = "SES-1";
    private static final String OPERATION_ID = "BOP-1";
    private static final String RELATION_ID = "REL-1";

    private static final String CATALOG_VERSION = "v1";
    private static final String ITEM = "GPS_LOCATION";

    private static final PrivacyAuthorizationCatalog CATALOG = PrivacyAuthorizationCatalogPolicy.build(
            CATALOG_VERSION, List.of(
                    new PrivacyAuthorizationCatalogPolicy.RawItem(ITEM, "位置信息", "Location Information", false),
                    new PrivacyAuthorizationCatalogPolicy.RawItem("DRIVING_BEHAVIOR", "驾驶行为分析",
                            "Driving Behavior Analysis", false)));

    @Mock
    private ConsentRepository consentRepository;
    @Mock
    private PrivacyAuthorizationRepository privacyAuthorizationRepository;
    @Mock
    private ActivationSessionRepository activationSessionRepository;
    @Mock
    private UserVehicleAuditRepository userVehicleAuditRepository;
    @Mock
    private UserVehicleRelationRepository userVehicleRelationRepository;
    @Mock
    private ActivationLoginTicketAppService activationLoginTicketAppService;
    @Mock
    private PlatformTransactionManager transactionManager;

    private ConsentAppService consentAppService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        lenient().when(userVehicleRelationRepository.findActiveByAccountAndVin(ACCOUNT, VIN))
                .thenReturn(Optional.of(UserVehicleRelation.builder()
                        .relationId(RELATION_ID).accountId(ACCOUNT).vin(VIN).state("ACTIVE").build()));
        consentAppService = new ConsentAppService(consentRepository, privacyAuthorizationRepository,
                activationSessionRepository, userVehicleAuditRepository, userVehicleRelationRepository,
                activationLoginTicketAppService,
                CATALOG, new TspUserVehicleProperties(), new TransactionTemplate(transactionManager),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private List<AgreementAcceptanceRequest> agreements() {
        return List.of(
                AgreementAcceptanceRequest.builder().agreementType(AgreementType.PRIVACY_POLICY).agreed(true)
                        .version("1.0").revisionId("REV-P").language("zh-CN").build(),
                AgreementAcceptanceRequest.builder().agreementType(AgreementType.USER_AGREEMENT).agreed(true)
                        .version("1.0").revisionId("REV-U").language("zh-CN").build());
    }

    @Test
    @DisplayName("跳过激活：只写 VIN 级同意，不创建用户关系与激活事实")
    void skipWritesVinConsentOnly() {
        when(activationSessionRepository.findBySessionId(anyString())).thenReturn(Optional.empty());
        SkipActivationRequest request = SkipActivationRequest.builder()
                .reportId("RPT-1").sourceSessionId(SESSION_ID).agreements(agreements()).build();

        var ack = consentAppService.skipActivation(VIN, request);

        assertThat(ack.getAccepted()).isTrue();
        verify(consentRepository, org.mockito.Mockito.times(2)).insertVinConsent(any(AgreementConsent.class));
        verify(consentRepository, never()).insertAccountConsent(any(AccountAgreementConsent.class));
        verify(userVehicleAuditRepository).insert(any());
    }

    @Test
    @DisplayName("未明确同意：拒绝记录")
    void skipRejectsNotAgreed() {
        var notAgreed = agreements();
        notAgreed.get(0).setAgreed(false);
        SkipActivationRequest request = SkipActivationRequest.builder()
                .reportId("RPT-1").agreements(notAgreed).build();

        assertThatThrownBy(() -> consentAppService.skipActivation(VIN, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.AGREEMENT_NOT_ACCEPTED.getCode()));
    }

    @Test
    @DisplayName("登录成功回执：按 Account+VIN 记录两项同意（与会话展示修订一致）")
    void loginSuccessWritesAccountConsent() {
        ActivationSession session = ActivationSession.builder()
                .sessionId(SESSION_ID).vin(VIN).claimedAccountId(ACCOUNT)
                .state(ActivationSessionState.BOUND).bindingOperationId(OPERATION_ID)
                .agreementRevisions("PRIVACY_POLICY:REV-P,USER_AGREEMENT:REV-U")
                .expiresAt(NOW.plusSeconds(600)).build();
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));
        when(activationSessionRepository.reportIviLoginResult(eq(SESSION_ID), any(), eq("RPT-2"), any()))
                .thenReturn(true);
        when(consentRepository.findAccountConsent(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        var ack = consentAppService.reportIviLoginResult(VIN, ReportIviLoginResultRequest.builder()
                .sessionId(SESSION_ID).bindingOperationId(OPERATION_ID).reportId("RPT-2")
                .loginStatus(IviLoginResultStatus.SUCCEEDED).agreements(agreements()).build());

        assertThat(ack.getAccepted()).isTrue();
        verify(consentRepository, org.mockito.Mockito.times(2)).insertAccountConsent(any(AccountAgreementConsent.class));
        verify(consentRepository, never()).insertVinConsent(any());
        // D019-05：登录成功写入与未消费票据作废在同一事务（同一编排调用）
        verify(activationLoginTicketAppService).revokeIssuedOnLoginSuccess(SESSION_ID, VIN, NOW);
    }

    @Test
    @DisplayName("登录回执协议修订与会话展示不一致：拒绝伪造同意")
    void loginRejectsRevisionMismatch() {
        ActivationSession session = ActivationSession.builder()
                .sessionId(SESSION_ID).vin(VIN).claimedAccountId(ACCOUNT)
                .state(ActivationSessionState.BOUND).bindingOperationId(OPERATION_ID)
                .agreementRevisions("PRIVACY_POLICY:REV-OTHER,USER_AGREEMENT:REV-OTHER")
                .expiresAt(NOW.plusSeconds(600)).build();
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));
        when(activationSessionRepository.reportIviLoginResult(anyString(), any(), anyString(), any())).thenReturn(true);

        assertThatThrownBy(() -> consentAppService.reportIviLoginResult(VIN,
                ReportIviLoginResultRequest.builder().sessionId(SESSION_ID)
                        .bindingOperationId(OPERATION_ID).reportId("RPT-2")
                        .loginStatus(IviLoginResultStatus.SUCCEEDED).agreements(agreements()).build()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.AGREEMENT_NOT_ACCEPTED.getCode()));
        verify(consentRepository, never()).insertAccountConsent(any());
    }

    @Test
    @DisplayName("登录失败：只记录登录结果，不写同意")
    void loginFailureDoesNotWriteConsent() {
        ActivationSession session = ActivationSession.builder()
                .sessionId(SESSION_ID).vin(VIN).claimedAccountId(ACCOUNT)
                .state(ActivationSessionState.BOUND).bindingOperationId(OPERATION_ID).build();
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));
        when(activationSessionRepository.reportIviLoginResult(anyString(), any(), anyString(), any())).thenReturn(true);

        consentAppService.reportIviLoginResult(VIN, ReportIviLoginResultRequest.builder()
                .sessionId(SESSION_ID).bindingOperationId(OPERATION_ID).reportId("RPT-3")
                .loginStatus(IviLoginResultStatus.FAILED).build());

        verify(consentRepository, never()).insertAccountConsent(any());
        verify(consentRepository, never()).insertVinConsent(any());
        // 登录失败不解除绑定、不作废票据（失败后可重签，受会话级限流约束）
        verify(activationLoginTicketAppService, never()).revokeIssuedOnLoginSuccess(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("授权旧版本/重复回执：不覆盖新状态")
    void privacyOlderVersionIgnored() {
        when(privacyAuthorizationRepository.find(ACCOUNT, VIN, RELATION_ID, ITEM))
                .thenReturn(Optional.of(PrivacyAuthorization.builder()
                        .authorizationId("PAZ-1").accountId(ACCOUNT).vin(VIN).authorizationItem(ITEM)
                        .state(PrivacyAuthorizationState.GRANTED).version(5L).reportId("RPT-OLD")
                        .payloadDigest("digest-old").build()));

        consentAppService.reportPrivacyAuthorization(ACCOUNT, VIN, privacyRequest("RPT-4", CATALOG_VERSION,
                ReportPrivacyAuthorizationRequest.AuthorizationItem.builder().authorizationItem(ITEM)
                        .state(PrivacyAuthorizationState.DENIED).version(3L).build()));

        verify(privacyAuthorizationRepository, never()).updateIfNewer(any());
        verify(privacyAuthorizationRepository, never()).insert(any());
    }

    @Test
    @DisplayName("关闭弹框不等于授权：NOT_GRANTED 原样记录")
    void privacyNotGrantedIsNotGranted() {
        when(privacyAuthorizationRepository.find(ACCOUNT, VIN, RELATION_ID, ITEM)).thenReturn(Optional.empty());

        consentAppService.reportPrivacyAuthorization(ACCOUNT, VIN, privacyRequest("RPT-5", CATALOG_VERSION,
                ReportPrivacyAuthorizationRequest.AuthorizationItem.builder().authorizationItem(ITEM)
                        .state(PrivacyAuthorizationState.NOT_GRANTED).version(1L).build()));

        verify(privacyAuthorizationRepository).insert(org.mockito.ArgumentMatchers.argThat(
                item -> item.getState() == PrivacyAuthorizationState.NOT_GRANTED
                        && RELATION_ID.equals(item.getRelationId())));
    }

    @Test
    @DisplayName("INVALIDATED 不接受车端上报：401112 且零写入")
    void privacyInvalidatedRejected() {
        assertThatThrownBy(() -> consentAppService.reportPrivacyAuthorization(ACCOUNT, VIN,
                privacyRequest("RPT-22", CATALOG_VERSION,
                        ReportPrivacyAuthorizationRequest.AuthorizationItem.builder().authorizationItem(ITEM)
                                .state(PrivacyAuthorizationState.INVALIDATED).version(1L).build())))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.INVALID_REQUEST.getCode()));
        verify(privacyAuthorizationRepository, never()).insert(any());
        verify(privacyAuthorizationRepository, never()).updateIfNewer(any());
    }

    @Test
    @DisplayName("无当前 ACTIVE 关系：拒绝授权生效且零写入（IOV-TSP-DSN-CR-021 D021-08）")
    void privacyWithoutActiveRelationRejected() {
        when(userVehicleRelationRepository.findActiveByAccountAndVin(ACCOUNT, VIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consentAppService.reportPrivacyAuthorization(ACCOUNT, VIN,
                privacyRequest("RPT-21", CATALOG_VERSION, item(ITEM, 1L))))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.INVALID_REQUEST.getCode()));
        verify(privacyAuthorizationRepository, never()).find(anyString(), anyString(), anyString(), anyString());
        verify(privacyAuthorizationRepository, never()).insert(any());
        verify(privacyAuthorizationRepository, never()).updateIfNewer(any());
    }

    @Test
    @DisplayName("同回执ID不同内容：冲突，不静默覆盖")
    void privacyReportConflict() {
        when(privacyAuthorizationRepository.find(ACCOUNT, VIN, RELATION_ID, ITEM))
                .thenReturn(Optional.of(PrivacyAuthorization.builder()
                        .authorizationId("PAZ-1").accountId(ACCOUNT).vin(VIN).authorizationItem(ITEM)
                        .state(PrivacyAuthorizationState.GRANTED).version(1L).reportId("RPT-6")
                        .payloadDigest("different-digest").build()));

        assertThatThrownBy(() -> consentAppService.reportPrivacyAuthorization(ACCOUNT, VIN,
                privacyRequest("RPT-6", CATALOG_VERSION,
                        ReportPrivacyAuthorizationRequest.AuthorizationItem.builder().authorizationItem(ITEM)
                                .state(PrivacyAuthorizationState.DENIED).version(2L).build())))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.REPORT_CONFLICT.getCode()));
    }

    // ================= D020-03：上报两阶段强校验（401119 / 401118） =================

    private ReportPrivacyAuthorizationRequest privacyRequest(
            String reportId, String catalogVersion,
            ReportPrivacyAuthorizationRequest.AuthorizationItem... items) {
        return ReportPrivacyAuthorizationRequest.builder()
                .reportId(reportId)
                .catalogVersion(catalogVersion)
                .items(List.of(items))
                .build();
    }

    private ReportPrivacyAuthorizationRequest.AuthorizationItem item(String code, long version) {
        return ReportPrivacyAuthorizationRequest.AuthorizationItem.builder()
                .authorizationItem(code).state(PrivacyAuthorizationState.GRANTED).version(version).build();
    }

    @Test
    @DisplayName("目录版本不一致：401119 整批拒绝、零写入")
    void privacyCatalogVersionMismatchRejected() {
        assertThatThrownBy(() -> consentAppService.reportPrivacyAuthorization(ACCOUNT, VIN,
                privacyRequest("RPT-10", "v0", item(ITEM, 1L))))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.CATALOG_VERSION_MISMATCH.getCode()));
        verify(privacyAuthorizationRepository, never()).find(anyString(), anyString(), anyString(), anyString());
        verify(privacyAuthorizationRepository, never()).insert(any());
        verify(privacyAuthorizationRepository, never()).updateIfNewer(any());
        verify(userVehicleAuditRepository, never()).insert(any());
    }

    @Test
    @DisplayName("catalogVersion 缺失：401112 且零写入")
    void privacyCatalogVersionMissingRejected() {
        assertThatThrownBy(() -> consentAppService.reportPrivacyAuthorization(ACCOUNT, VIN,
                privacyRequest("RPT-11", null, item(ITEM, 1L))))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.INVALID_REQUEST.getCode()));
        verify(privacyAuthorizationRepository, never()).insert(any());
    }

    // ================= 终审修复 B-1：enforce-catalog 两开关 × 存量/新报文 =================

    /**
     * 构造指定开关组合下的服务实例（B-1：只有 enabled && enforceCatalog 同时为 true 才强校验）
     */
    private ConsentAppService serviceWith(boolean enabled, boolean enforceCatalog) {
        TspUserVehicleProperties props = new TspUserVehicleProperties();
        props.getPrivacyAuthorization().setEnabled(enabled);
        props.getPrivacyAuthorization().setEnforceCatalog(enforceCatalog);
        return new ConsentAppService(consentRepository, privacyAuthorizationRepository, activationSessionRepository,
                userVehicleAuditRepository, userVehicleRelationRepository, activationLoginTicketAppService, CATALOG,
                props, new TransactionTemplate(transactionManager), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("B-1 兼容：enforce-catalog=false 时存量报文（无 catalogVersion）可上报，且审计标记 COMPAT_MODE")
    void compatModeAcceptsLegacyReportWithoutCatalogVersion() {
        ConsentAppService compat = serviceWith(true, false);

        var ack = compat.reportPrivacyAuthorization(ACCOUNT, VIN, privacyRequest("RPT-30", null, item(ITEM, 1L)));

        assertThat(ack.getAccepted()).isTrue();
        verify(privacyAuthorizationRepository).insert(any(PrivacyAuthorization.class));
        verify(userVehicleAuditRepository).insert(org.mockito.ArgumentMatchers.argThat(
                audit -> "PRIVACY_REPORT/COMPAT_MODE".equals(audit.getToState())));
    }

    @Test
    @DisplayName("B-1 兼容：enabled=false 时存量报文（无 catalogVersion）也可上报（回退旧行为）")
    void catalogDisabledAcceptsLegacyReportWithoutCatalogVersion() {
        ConsentAppService compat = serviceWith(false, true);

        var ack = compat.reportPrivacyAuthorization(ACCOUNT, VIN, privacyRequest("RPT-31", null, item(ITEM, 1L)));

        assertThat(ack.getAccepted()).isTrue();
        verify(privacyAuthorizationRepository).insert(any(PrivacyAuthorization.class));
        verify(userVehicleAuditRepository).insert(org.mockito.ArgumentMatchers.argThat(
                audit -> "PRIVACY_REPORT/COMPAT_MODE".equals(audit.getToState())));
    }

    @Test
    @DisplayName("B-1 兼容：enforce-catalog=false 时不校验目录版本与目录外项（旧报文目录外项不报 401118）")
    void compatModeSkipsCatalogVersionAndUnknownItemChecks() {
        ConsentAppService compat = serviceWith(true, false);

        var ack = compat.reportPrivacyAuthorization(ACCOUNT, VIN,
                privacyRequest("RPT-32", "v0", item(ITEM, 1L), item("UNKNOWN_ITEM", 1L)));

        assertThat(ack.getAccepted()).isTrue();
        verify(privacyAuthorizationRepository, org.mockito.Mockito.times(2))
                .insert(any(PrivacyAuthorization.class));
    }

    @Test
    @DisplayName("B-1 强校验：enforce-catalog=true 时新报文仍被强校验（目录版本不等 401119 / 目录外项 401118）")
    void enforceModeStillValidatesNewReport() {
        ConsentAppService enforced = serviceWith(true, true);

        assertThatThrownBy(() -> enforced.reportPrivacyAuthorization(ACCOUNT, VIN,
                privacyRequest("RPT-33", "v0", item(ITEM, 1L))))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.CATALOG_VERSION_MISMATCH.getCode()));
        assertThatThrownBy(() -> enforced.reportPrivacyAuthorization(ACCOUNT, VIN,
                privacyRequest("RPT-34", CATALOG_VERSION, item(ITEM, 1L), item("UNKNOWN_ITEM", 1L))))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.PRIVACY_ITEM_NOT_APPROVED.getCode()));
        // 强校验生效时 catalogVersion 缺失仍为 401112
        assertThatThrownBy(() -> enforced.reportPrivacyAuthorization(ACCOUNT, VIN,
                privacyRequest("RPT-35", null, item(ITEM, 1L))))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.INVALID_REQUEST.getCode()));
        verify(privacyAuthorizationRepository, never()).insert(any());
        verify(userVehicleAuditRepository, never()).insert(any());
    }

    @Test
    @DisplayName("B-1 强校验顺序保持：字段非法（401112）先于目录版本不等（401119）与目录外项（401118）")
    void enforceModeKeepsValidationOrder() {
        ConsentAppService enforced = serviceWith(true, true);

        assertThatThrownBy(() -> enforced.reportPrivacyAuthorization(ACCOUNT, VIN,
                privacyRequest("RPT-36", "v0", item("UNKNOWN_ITEM", 0L))))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.INVALID_REQUEST.getCode()));
    }

    @Test
    @DisplayName("目录外授权项：401118 且整批拒绝（第 2 项非法时第 1 项不得落库）")
    void privacyUnknownItemRejectsWholeBatch() {
        assertThatThrownBy(() -> consentAppService.reportPrivacyAuthorization(ACCOUNT, VIN,
                privacyRequest("RPT-12", CATALOG_VERSION, item(ITEM, 1L), item("UNKNOWN_ITEM", 1L))))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.PRIVACY_ITEM_NOT_APPROVED.getCode()));
        verify(privacyAuthorizationRepository, never()).insert(any());
        verify(privacyAuthorizationRepository, never()).updateIfNewer(any());
        verify(userVehicleAuditRepository, never()).insert(any());
    }

    @Test
    @DisplayName("校验顺序固定：字段非法（401112）先于目录版本不等（401119）")
    void privacyValidationOrderInvalidRequestFirst() {
        assertThatThrownBy(() -> consentAppService.reportPrivacyAuthorization(ACCOUNT, VIN,
                privacyRequest("RPT-13", "v0", item(ITEM, 0L))))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.INVALID_REQUEST.getCode()));
    }

    @Test
    @DisplayName("决策序号大于已存：更新为最新状态")
    void privacyNewerVersionUpdates() {
        when(privacyAuthorizationRepository.find(ACCOUNT, VIN, RELATION_ID, ITEM))
                .thenReturn(Optional.of(PrivacyAuthorization.builder()
                        .authorizationId("PAZ-1").accountId(ACCOUNT).vin(VIN).authorizationItem(ITEM)
                        .state(PrivacyAuthorizationState.GRANTED).version(3L).reportId("RPT-OLD")
                        .payloadDigest("digest-old").build()));

        consentAppService.reportPrivacyAuthorization(ACCOUNT, VIN, privacyRequest("RPT-14", CATALOG_VERSION,
                ReportPrivacyAuthorizationRequest.AuthorizationItem.builder().authorizationItem(ITEM)
                        .state(PrivacyAuthorizationState.DENIED).version(4L).build()));

        verify(privacyAuthorizationRepository).updateIfNewer(org.mockito.ArgumentMatchers.argThat(
                item -> item.getVersion() == 4L && item.getState() == PrivacyAuthorizationState.DENIED));
    }

    @Test
    @DisplayName("决策序号等于已存：幂等跳过且不报错")
    void privacyEqualVersionIsIdempotent() {
        when(privacyAuthorizationRepository.find(ACCOUNT, VIN, RELATION_ID, ITEM))
                .thenReturn(Optional.of(PrivacyAuthorization.builder()
                        .authorizationId("PAZ-1").accountId(ACCOUNT).vin(VIN).authorizationItem(ITEM)
                        .state(PrivacyAuthorizationState.GRANTED).version(3L).reportId("RPT-OLD")
                        .payloadDigest("digest-old").build()));

        consentAppService.reportPrivacyAuthorization(ACCOUNT, VIN, privacyRequest("RPT-15", CATALOG_VERSION,
                item(ITEM, 3L)));

        verify(privacyAuthorizationRepository, never()).updateIfNewer(any());
        verify(privacyAuthorizationRepository, never()).insert(any());
    }

    // ================= D020-06：FAILED 回执、R-1 终态保护、R-2 审计仅首次 =================

    private ActivationSession boundSession() {
        return ActivationSession.builder()
                .sessionId(SESSION_ID).vin(VIN).claimedAccountId(ACCOUNT)
                .state(ActivationSessionState.BOUND).bindingOperationId(OPERATION_ID)
                .agreementRevisions("PRIVACY_POLICY:REV-P,USER_AGREEMENT:REV-U")
                .expiresAt(NOW.plusSeconds(600)).rowVersion(1).build();
    }

    @Test
    @DisplayName("SUCCEEDED 缺 bindingOperationId：401112 且不写任何事实")
    void loginSucceededRequiresBindingOperationId() {
        when(activationSessionRepository.findBySessionId(SESSION_ID))
                .thenReturn(Optional.of(boundSession()));

        assertThatThrownBy(() -> consentAppService.reportIviLoginResult(VIN,
                ReportIviLoginResultRequest.builder().sessionId(SESSION_ID).reportId("RPT-20")
                        .loginStatus(IviLoginResultStatus.SUCCEEDED).agreements(agreements()).build()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.INVALID_REQUEST.getCode()));
        verify(activationSessionRepository, never()).reportIviLoginResult(anyString(), any(), anyString(), any());
    }

    @Test
    @DisplayName("FAILED 可缺 bindingOperationId：仍记录失败事实与审计")
    void loginFailedWithoutBindingOperationIdRecorded() {
        when(activationSessionRepository.findBySessionId(SESSION_ID))
                .thenReturn(Optional.of(boundSession()));
        when(activationSessionRepository.reportIviLoginResult(eq(SESSION_ID),
                eq(IviLoginResultStatus.FAILED), eq("RPT-21"), any())).thenReturn(true);

        var ack = consentAppService.reportIviLoginResult(VIN, ReportIviLoginResultRequest.builder()
                .sessionId(SESSION_ID).reportId("RPT-21").loginStatus(IviLoginResultStatus.FAILED)
                .failureReason(IviLoginFailureReason.TOKEN_EXCHANGE_FAILED).build());

        assertThat(ack.getAccepted()).isTrue();
        verify(activationSessionRepository).reportIviLoginResult(eq(SESSION_ID),
                eq(IviLoginResultStatus.FAILED), eq("RPT-21"), any());
        verify(consentRepository, never()).insertAccountConsent(any());
        verify(activationLoginTicketAppService, never())
                .revokeIssuedOnLoginSuccess(anyString(), anyString(), any());
        verify(userVehicleAuditRepository).insert(org.mockito.ArgumentMatchers.argThat(
                audit -> "FAILED:TOKEN_EXCHANGE_FAILED".equals(audit.getReason())));
    }

    @Test
    @DisplayName("FAILED 缺 failureReason：审计 reason 记 FAILED:-")
    void loginFailedWithoutReasonAuditsDash() {
        when(activationSessionRepository.findBySessionId(SESSION_ID))
                .thenReturn(Optional.of(boundSession()));
        when(activationSessionRepository.reportIviLoginResult(anyString(), any(), anyString(), any()))
                .thenReturn(true);

        consentAppService.reportIviLoginResult(VIN, ReportIviLoginResultRequest.builder()
                .sessionId(SESSION_ID).reportId("RPT-22").loginStatus(IviLoginResultStatus.FAILED).build());

        verify(userVehicleAuditRepository).insert(org.mockito.ArgumentMatchers.argThat(
                audit -> "FAILED:-".equals(audit.getReason())));
    }

    @Test
    @DisplayName("FAILED 绑定操作号与会话不一致：仍记录失败（E2 定稿不返回 4xx）且审计标注")
    void loginFailedWithMismatchedOperationIdStillRecorded() {
        when(activationSessionRepository.findBySessionId(SESSION_ID))
                .thenReturn(Optional.of(boundSession()));
        when(activationSessionRepository.reportIviLoginResult(anyString(), any(), anyString(), any()))
                .thenReturn(true);

        var ack = consentAppService.reportIviLoginResult(VIN, ReportIviLoginResultRequest.builder()
                .sessionId(SESSION_ID).bindingOperationId("BOP-OTHER").reportId("RPT-23")
                .loginStatus(IviLoginResultStatus.FAILED)
                .failureReason(IviLoginFailureReason.ACCOUNT_MISMATCH).build());

        assertThat(ack.getAccepted()).isTrue();
        verify(userVehicleAuditRepository).insert(org.mockito.ArgumentMatchers.argThat(
                audit -> audit.getReason().contains("FAILED_BINDING_OP_MISMATCH") 
                        && audit.getReason().contains("FAILED:ACCOUNT_MISMATCH")));
    }

    @Test
    @DisplayName("R-1 终态保护：会话已 SUCCEEDED 时迟到 FAILED 不回退、不写审计、不重开票据窗口")
    void lateFailedDoesNotOverrideSucceeded() {
        ActivationSession session = boundSession();
        session.setIviLoginState(IviLoginResultStatus.SUCCEEDED);
        session.setIviLoginReportId("RPT-EARLY");
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));
        // 终态保护 SQL：ivi_login_state = 'SUCCEEDED' 时条件更新影响 0 行
        when(activationSessionRepository.reportIviLoginResult(anyString(), any(), anyString(), any()))
                .thenReturn(false);

        var ack = consentAppService.reportIviLoginResult(VIN, ReportIviLoginResultRequest.builder()
                .sessionId(SESSION_ID).reportId("RPT-LATE").loginStatus(IviLoginResultStatus.FAILED)
                .failureReason(IviLoginFailureReason.TIMEOUT).build());

        assertThat(ack.getAccepted()).isTrue();
        verify(userVehicleAuditRepository, never()).insert(any());
        verify(consentRepository, never()).insertAccountConsent(any());
        verify(activationLoginTicketAppService, never())
                .revokeIssuedOnLoginSuccess(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("R-2 审计仅首次：同 reportId 重放不再新增审计行")
    void duplicateReportDoesNotAuditTwice() {
        ActivationSession session = boundSession();
        session.setIviLoginState(IviLoginResultStatus.SUCCEEDED);
        session.setIviLoginReportId("RPT-24");
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));

        var ack = consentAppService.reportIviLoginResult(VIN, ReportIviLoginResultRequest.builder()
                .sessionId(SESSION_ID).bindingOperationId(OPERATION_ID).reportId("RPT-24")
                .loginStatus(IviLoginResultStatus.SUCCEEDED).agreements(agreements()).build());

        assertThat(ack.getAccepted()).isTrue();
        verify(activationSessionRepository, never()).reportIviLoginResult(anyString(), any(), anyString(), any());
        verify(userVehicleAuditRepository, never()).insert(any());
        verify(consentRepository, never()).insertAccountConsent(any());
    }
}
