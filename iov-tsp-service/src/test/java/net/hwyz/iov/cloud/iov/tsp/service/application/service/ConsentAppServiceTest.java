package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ActivationSessionState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.AgreementType;
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
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationSessionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ConsentRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.PrivacyAuthorizationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleAuditRepository;
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
 * 同意与隐私授权测试（IOV-TSP-DSN-CR-017 TC017-06/TC017-07）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsentAppServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");
    private static final String VIN = "LSVAA1234567890AB";
    private static final String ACCOUNT = "account-1";
    private static final String SESSION_ID = "SES-1";
    private static final String OPERATION_ID = "BOP-1";

    @Mock
    private ConsentRepository consentRepository;
    @Mock
    private PrivacyAuthorizationRepository privacyAuthorizationRepository;
    @Mock
    private ActivationSessionRepository activationSessionRepository;
    @Mock
    private UserVehicleAuditRepository userVehicleAuditRepository;
    @Mock
    private ActivationLoginTicketAppService activationLoginTicketAppService;
    @Mock
    private PlatformTransactionManager transactionManager;

    private ConsentAppService consentAppService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        consentAppService = new ConsentAppService(consentRepository, privacyAuthorizationRepository,
                activationSessionRepository, userVehicleAuditRepository, activationLoginTicketAppService,
                new TransactionTemplate(transactionManager), Clock.fixed(NOW, ZoneOffset.UTC));
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
        when(privacyAuthorizationRepository.find(ACCOUNT, VIN, "LOCATION"))
                .thenReturn(Optional.of(PrivacyAuthorization.builder()
                        .authorizationId("PAZ-1").accountId(ACCOUNT).vin(VIN).authorizationItem("LOCATION")
                        .state(PrivacyAuthorizationState.GRANTED).version(5L).reportId("RPT-OLD")
                        .payloadDigest("digest-old").build()));

        consentAppService.reportPrivacyAuthorization(ACCOUNT, VIN, ReportPrivacyAuthorizationRequest.builder()
                .reportId("RPT-4")
                .items(List.of(ReportPrivacyAuthorizationRequest.AuthorizationItem.builder()
                        .authorizationItem("LOCATION").state(PrivacyAuthorizationState.DENIED).version(3L).build()))
                .build());

        verify(privacyAuthorizationRepository, never()).updateIfNewer(any());
        verify(privacyAuthorizationRepository, never()).insert(any());
    }

    @Test
    @DisplayName("关闭弹框不等于授权：NOT_GRANTED 原样记录")
    void privacyNotGrantedIsNotGranted() {
        when(privacyAuthorizationRepository.find(ACCOUNT, VIN, "LOCATION")).thenReturn(Optional.empty());

        consentAppService.reportPrivacyAuthorization(ACCOUNT, VIN, ReportPrivacyAuthorizationRequest.builder()
                .reportId("RPT-5")
                .items(List.of(ReportPrivacyAuthorizationRequest.AuthorizationItem.builder()
                        .authorizationItem("LOCATION").state(PrivacyAuthorizationState.NOT_GRANTED).version(1L).build()))
                .build());

        verify(privacyAuthorizationRepository).insert(org.mockito.ArgumentMatchers.argThat(
                item -> item.getState() == PrivacyAuthorizationState.NOT_GRANTED));
    }

    @Test
    @DisplayName("同回执ID不同内容：冲突，不静默覆盖")
    void privacyReportConflict() {
        when(privacyAuthorizationRepository.find(ACCOUNT, VIN, "LOCATION"))
                .thenReturn(Optional.of(PrivacyAuthorization.builder()
                        .authorizationId("PAZ-1").accountId(ACCOUNT).vin(VIN).authorizationItem("LOCATION")
                        .state(PrivacyAuthorizationState.GRANTED).version(1L).reportId("RPT-6")
                        .payloadDigest("different-digest").build()));

        assertThatThrownBy(() -> consentAppService.reportPrivacyAuthorization(ACCOUNT, VIN,
                ReportPrivacyAuthorizationRequest.builder().reportId("RPT-6")
                        .items(List.of(ReportPrivacyAuthorizationRequest.AuthorizationItem.builder()
                                .authorizationItem("LOCATION").state(PrivacyAuthorizationState.DENIED)
                                .version(2L).build())).build()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.REPORT_CONFLICT.getCode()));
    }
}
