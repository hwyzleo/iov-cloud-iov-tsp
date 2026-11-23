package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ActivationSessionState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingCallerScope;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingOperationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingResultReason;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.RelationSource;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UserVehicleRelationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.ConfirmVehicleBindingRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ConfirmVehicleBindingVo;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.UserVehicleBindingErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.ActivationPayloadDigest;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.DigestUtils;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationFact;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationSession;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationSessionIdempotency;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.BindingOperation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.DeliveryFacts;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.DeliveryFactsQuality;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleRelation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.VehicleFacts;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.AgreementContentPort;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.DependencyUnavailableException;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.OrderDeliveryFactsPort;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.VehicleFactsPort;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationFactRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationSessionIdempotencyRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationSessionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.BindingOperationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleAuditRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleRelationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.service.BindingEligibilityPolicy;
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

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.AgreementType;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.AgreementAcceptanceRequest;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AgreementBundle;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AgreementContent;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 用户绑车确认编排测试（IOV-TSP-DSN-CR-017 TC017-04）。
 * <p>
 * 覆盖幂等重放、同键异载荷冲突、依赖不可用保留可重试、资格拒绝、关系冲突与原子提交。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserVehicleBindingAppServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");
    private static final String ACCOUNT = "account-1";
    private static final String VIN = "LSVAA1234567890AB";
    private static final String SESSION_ID = "SES-1";
    private static final String KEY = "idem-1";

    @Mock
    private AgreementContentPort agreementContentPort;
    @Mock
    private VehicleFactsPort vehicleFactsPort;
    @Mock
    private OrderDeliveryFactsPort orderDeliveryFactsPort;
    @Mock
    private ActivationSessionRepository activationSessionRepository;
    @Mock
    private ActivationSessionIdempotencyRepository activationSessionIdempotencyRepository;
    @Mock
    private UserVehicleRelationRepository userVehicleRelationRepository;
    @Mock
    private BindingOperationRepository bindingOperationRepository;
    @Mock
    private ActivationFactRepository activationFactRepository;
    @Mock
    private UserVehicleAuditRepository userVehicleAuditRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private UserVehicleBindingAppService appService;
    private final AtomicReference<BindingOperation> insertedOperation = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        TspUserVehicleProperties properties = new TspUserVehicleProperties();
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        appService = new UserVehicleBindingAppService(agreementContentPort, vehicleFactsPort, orderDeliveryFactsPort,
                activationSessionRepository, activationSessionIdempotencyRepository, userVehicleRelationRepository,
                bindingOperationRepository, activationFactRepository, userVehicleAuditRepository,
                new BindingEligibilityPolicy(), properties, new TransactionTemplate(transactionManager),
                Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().doAnswer(invocation -> {
            BindingOperation operation = invocation.getArgument(0);
            operation.setId(1L);
            insertedOperation.set(operation);
            return null;
        }).when(bindingOperationRepository).insert(any(BindingOperation.class));
        lenient().when(bindingOperationRepository.lockByIdempotencyKey(eq(BindingCallerScope.APP), eq(ACCOUNT),
                anyString())).thenAnswer(invocation -> Optional.ofNullable(insertedOperation.get()));
    }

    private ConfirmVehicleBindingRequest request() {
        return ConfirmVehicleBindingRequest.builder().sessionId(SESSION_ID).idempotencyKey(KEY).build();
    }

    private ActivationSession scannedSession() {
        return ActivationSession.builder()
                .sessionId(SESSION_ID)
                .vin(VIN)
                .state(ActivationSessionState.SCANNED)
                .claimedAccountId(ACCOUNT)
                .expiresAt(NOW.plusSeconds(600))
                .rowVersion(1)
                .build();
    }

    private DeliveryFacts deliveredFacts() {
        return new DeliveryFacts(DeliveryFactsQuality.MATCHED, true, List.of("order_user"),
                "ORDER-1", "order=ORDER-1", NOW);
    }

    @Test
    @DisplayName("资格通过：原子提交关系、会话 BOUND 与操作成功，并登记首次激活候选事实")
    void confirmSuccessCommitsRelationAndFact() {
        when(bindingOperationRepository.findByIdempotencyKey(any(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(scannedSession()));
        when(activationSessionRepository.lockBySessionId(SESSION_ID)).thenReturn(Optional.of(scannedSession()));
        when(vehicleFactsPort.load(VIN)).thenReturn(VehicleFacts.present(VIN));
        when(orderDeliveryFactsPort.load(ACCOUNT, VIN)).thenReturn(deliveredFacts());
        when(userVehicleRelationRepository.findActiveByVin(VIN)).thenReturn(Optional.empty());
        when(activationSessionRepository.markBound(eq(SESSION_ID), anyString(), any(), any(), eq(1))).thenReturn(true);
        when(bindingOperationRepository.markSucceeded(anyString(), anyString(), anyString(), any(), any(),
                anyInt())).thenReturn(true);
        when(activationFactRepository.findByBusinessKey(anyString(), anyString())).thenReturn(Optional.empty());

        ConfirmVehicleBindingVo result = appService.confirm(ACCOUNT, request());

        assertThat(result.getStatus()).isEqualTo(BindingOperationStatus.SUCCEEDED);
        assertThat(result.getRelationId()).isNotBlank();
        assertThat(result.getVin()).isEqualTo(VIN);
        verify(userVehicleRelationRepository).insert(any(UserVehicleRelation.class));
        verify(activationFactRepository).insert(any());
    }

    @Test
    @DisplayName("首次激活候选事实摘要口径 = R-10：sha256(vin|bindingOperationId|sourceFactRef|occurredAt)，不含 eventId")
    void activationFactDigestUsesR10Definition() {
        when(bindingOperationRepository.findByIdempotencyKey(any(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(scannedSession()));
        when(activationSessionRepository.lockBySessionId(SESSION_ID)).thenReturn(Optional.of(scannedSession()));
        when(vehicleFactsPort.load(VIN)).thenReturn(VehicleFacts.present(VIN));
        when(orderDeliveryFactsPort.load(ACCOUNT, VIN)).thenReturn(deliveredFacts());
        when(userVehicleRelationRepository.findActiveByVin(VIN)).thenReturn(Optional.empty());
        when(activationSessionRepository.markBound(eq(SESSION_ID), anyString(), any(), any(), eq(1))).thenReturn(true);
        when(bindingOperationRepository.markSucceeded(anyString(), anyString(), anyString(), any(), any(),
                anyInt())).thenReturn(true);
        when(activationFactRepository.findByBusinessKey(anyString(), anyString())).thenReturn(Optional.empty());

        appService.confirm(ACCOUNT, request());

        org.mockito.ArgumentCaptor<ActivationFact> captor = org.mockito.ArgumentCaptor.forClass(ActivationFact.class);
        verify(activationFactRepository).insert(captor.capture());
        ActivationFact fact = captor.getValue();
        String expected = ActivationPayloadDigest.compute(fact.getVin(), fact.getBindingOperationId(),
                fact.getSourceFactRef(), fact.getOccurredAt());
        assertThat(fact.getPayloadDigest()).isEqualTo(expected);
        assertThat(fact.getSourceFactRef()).isEqualTo("order=ORDER-1");
        // 旧口径 sha256(eventId|vin|bindingOperationId) 已废弃：不得再产出旧摘要
        String legacy = DigestUtils.sha256Hex(
                String.join("|", fact.getEventId(), fact.getVin(), fact.getBindingOperationId()));
        assertThat(fact.getPayloadDigest()).isNotEqualTo(legacy);
    }

    @Test
    @DisplayName("同键同内容重放：直接返回既有结果，不重复创建关系")
    void replaySameKeyReturnsExistingResult() {
        String digest = DigestUtils.sha256Hex(
                String.join("|", BindingCallerScope.APP.name(), ACCOUNT, SESSION_ID));
        BindingOperation existing = BindingOperation.builder()
                .operationId("BOP-1").accountId(ACCOUNT).idempotencyKey(KEY).requestDigest(digest)
                .sessionId(SESSION_ID).vin(VIN).status(BindingOperationStatus.SUCCEEDED)
                .resultRelationId("REL-1").completedAt(NOW).build();
        when(bindingOperationRepository.findByIdempotencyKey(BindingCallerScope.APP, ACCOUNT, KEY))
                .thenReturn(Optional.of(existing));

        ConfirmVehicleBindingVo result = appService.confirm(ACCOUNT, request());

        assertThat(result.getStatus()).isEqualTo(BindingOperationStatus.SUCCEEDED);
        assertThat(result.getRelationId()).isEqualTo("REL-1");
        verify(userVehicleRelationRepository, never()).insert(any());
    }

    @Test
    @DisplayName("同键不同内容：返回幂等冲突")
    void replayDifferentPayloadConflicts() {
        BindingOperation existing = BindingOperation.builder()
                .operationId("BOP-1").accountId(ACCOUNT).idempotencyKey(KEY).requestDigest("other-digest")
                .sessionId(SESSION_ID).vin(VIN).status(BindingOperationStatus.SUCCEEDED).build();
        when(bindingOperationRepository.findByIdempotencyKey(BindingCallerScope.APP, ACCOUNT, KEY))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> appService.confirm(ACCOUNT, request()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.IDEMPOTENCY_CONFLICT.getCode()));
    }

    @Test
    @DisplayName("依赖不可用：不写终态、不创建关系，允许同键重试")
    void dependencyUnavailableKeepsRetryable() {
        when(bindingOperationRepository.findByIdempotencyKey(any(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(scannedSession()));
        when(vehicleFactsPort.load(VIN)).thenReturn(VehicleFacts.present(VIN));
        when(orderDeliveryFactsPort.load(ACCOUNT, VIN))
                .thenThrow(new DependencyUnavailableException("VSO timeout"));

        assertThatThrownBy(() -> appService.confirm(ACCOUNT, request()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.DEPENDENCY_UNAVAILABLE.getCode()));
        verify(userVehicleRelationRepository, never()).insert(any());
        verify(bindingOperationRepository, never()).markFailed(anyString(), any(), any(), anyString(), anyString(),
                any(), any(), anyInt());
    }

    @Test
    @DisplayName("资格不符：写入失败终态并拒绝，不创建关系")
    void eligibilityRejectedMarksFailed() {
        when(bindingOperationRepository.findByIdempotencyKey(any(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(scannedSession()));
        when(vehicleFactsPort.load(VIN)).thenReturn(VehicleFacts.present(VIN));
        when(orderDeliveryFactsPort.load(ACCOUNT, VIN)).thenReturn(
                new DeliveryFacts(DeliveryFactsQuality.MATCHED, false, List.of("order_user"), "ORDER-1", "f", NOW));
        when(activationSessionRepository.lockBySessionId(SESSION_ID)).thenReturn(Optional.of(scannedSession()));
        when(bindingOperationRepository.markFailed(anyString(), any(), any(), anyString(), anyString(), any(), any(),
                anyInt())).thenReturn(true);

        assertThatThrownBy(() -> appService.confirm(ACCOUNT, request()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.ELIGIBILITY_REJECTED.getCode()));
        verify(userVehicleRelationRepository, never()).insert(any());
    }

    @Test
    @DisplayName("该 VIN 已有其他账号主使用人：关系冲突，不替换不解绑")
    void relationConflictWithOtherAccount() {
        when(bindingOperationRepository.findByIdempotencyKey(any(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(scannedSession()));
        when(activationSessionRepository.lockBySessionId(SESSION_ID)).thenReturn(Optional.of(scannedSession()));
        when(vehicleFactsPort.load(VIN)).thenReturn(VehicleFacts.present(VIN));
        when(orderDeliveryFactsPort.load(ACCOUNT, VIN)).thenReturn(deliveredFacts());
        when(userVehicleRelationRepository.findActiveByVin(VIN)).thenReturn(Optional.of(
                UserVehicleRelation.builder().relationId("REL-OTHER").accountId("account-2").vin(VIN).build()));
        when(bindingOperationRepository.markFailed(anyString(), any(), any(), anyString(), anyString(), any(), any(),
                anyInt())).thenReturn(true);

        assertThatThrownBy(() -> appService.confirm(ACCOUNT, request()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.RELATION_CONFLICT.getCode()));
        verify(userVehicleRelationRepository, never()).insert(any());
    }

    @Test
    @DisplayName("同账号重复请求：幂等关联既有关系，不重复创建")
    void sameAccountIdempotentLink() {
        when(bindingOperationRepository.findByIdempotencyKey(any(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(scannedSession()));
        when(activationSessionRepository.lockBySessionId(SESSION_ID)).thenReturn(Optional.of(scannedSession()));
        when(vehicleFactsPort.load(VIN)).thenReturn(VehicleFacts.present(VIN));
        when(orderDeliveryFactsPort.load(ACCOUNT, VIN)).thenReturn(deliveredFacts());
        when(userVehicleRelationRepository.findActiveByVin(VIN)).thenReturn(Optional.of(
                UserVehicleRelation.builder().relationId("REL-EXIST").accountId(ACCOUNT).vin(VIN).build()));
        when(bindingOperationRepository.markSucceeded(anyString(), anyString(), anyString(), any(), any(),
                anyInt())).thenReturn(true);

        ConfirmVehicleBindingVo result = appService.confirm(ACCOUNT, request());

        assertThat(result.getStatus()).isEqualTo(BindingOperationStatus.SUCCEEDED);
        assertThat(result.getRelationId()).isEqualTo("REL-EXIST");
        verify(userVehicleRelationRepository, never()).insert(any());
    }

    @Test
    @DisplayName("会话未由本人认领：拒绝确认，不读取外部事实")
    void confirmRejectsSessionClaimedByOther() {
        when(bindingOperationRepository.findByIdempotencyKey(any(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        ActivationSession otherClaimed = scannedSession();
        otherClaimed.setClaimedAccountId("account-2");
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(otherClaimed));

        assertThatThrownBy(() -> appService.confirm(ACCOUNT, request()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.SESSION_CLAIMED.getCode()));
        verify(vehicleFactsPort, never()).load(anyString());
    }

    @Test
    @DisplayName("取消后确认：按当前会话状态拒绝")
    void confirmRejectsCancelledSession() {
        when(bindingOperationRepository.findByIdempotencyKey(any(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        ActivationSession cancelled = scannedSession();
        cancelled.setState(ActivationSessionState.CANCELLED);
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> appService.confirm(ACCOUNT, request()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.SESSION_CANCELLED.getCode()));
    }

    @Test
    @DisplayName("扫码时有效但确认前过期：不得提交绑定")
    void confirmRejectsExpiredSession() {
        when(bindingOperationRepository.findByIdempotencyKey(any(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        ActivationSession expired = scannedSession();
        expired.setExpiresAt(NOW.minusSeconds(1));
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> appService.confirm(ACCOUNT, request()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.SESSION_EXPIRED.getCode()));
    }

    // ================= 当前关系查询（IOV-TSP-DSN-CR-018） =================

    @Test
    @DisplayName("当前关系：存在有效关系返回 BOUND 且不读写其他事实")
    void queryCurrentRelationReturnsBound() {
        Instant boundAt = Instant.parse("2026-11-21T10:00:00Z");
        when(userVehicleRelationRepository.findActiveByAccountAndVin(ACCOUNT, VIN))
                .thenReturn(Optional.of(UserVehicleRelation.builder()
                        .relationId("REL-1").accountId(ACCOUNT).vin(VIN).role("PRIMARY_USER")
                        .state("ACTIVE").boundAt(boundAt).sourceOperationId("BOP-1")
                        .ruleVersion("R1").factRef("fact-1").rowVersion(1).build()));

        var result = appService.queryCurrentRelation(ACCOUNT, VIN);

        assertThat(result.getStatus()).isEqualTo(UserVehicleRelationStatus.BOUND);
        assertThat(result.getVin()).isEqualTo(VIN);
        assertThat(result.getRelationId()).isEqualTo("REL-1");
        assertThat(result.getRole()).isEqualTo("PRIMARY_USER");
        assertThat(result.getBoundAt()).isEqualTo(boundAt);
        verify(userVehicleRelationRepository).findActiveByAccountAndVin(ACCOUNT, VIN);
        verify(userVehicleRelationRepository, never()).findActiveByVin(anyString());
        verify(userVehicleRelationRepository, never()).insert(any());
        verifyNoInteractions(agreementContentPort, vehicleFactsPort, orderDeliveryFactsPort,
                activationSessionRepository, bindingOperationRepository, activationFactRepository,
                userVehicleAuditRepository, transactionManager);
    }

    @Test
    @DisplayName("当前关系：无有效关系返回 NOT_BOUND，不按 VIN 反查他人关系")
    void queryCurrentRelationReturnsNotBound() {
        when(userVehicleRelationRepository.findActiveByAccountAndVin(ACCOUNT, VIN)).thenReturn(Optional.empty());

        var result = appService.queryCurrentRelation(ACCOUNT, VIN);

        assertThat(result.getStatus()).isEqualTo(UserVehicleRelationStatus.NOT_BOUND);
        assertThat(result.getVin()).isEqualTo(VIN);
        assertThat(result.getRelationId()).isNull();
        assertThat(result.getRole()).isNull();
        assertThat(result.getBoundAt()).isNull();
        verify(userVehicleRelationRepository, never()).findActiveByVin(anyString());
        verify(userVehicleRelationRepository, never()).insert(any());
        verifyNoInteractions(agreementContentPort, vehicleFactsPort, orderDeliveryFactsPort,
                activationSessionRepository, bindingOperationRepository, activationFactRepository,
                userVehicleAuditRepository, transactionManager);
    }

    @Test
    @DisplayName("当前关系：数据库异常向上传递，不得伪装为 NOT_BOUND")
    void queryCurrentRelationPropagatesRepositoryFailure() {
        when(userVehicleRelationRepository.findActiveByAccountAndVin(ACCOUNT, VIN))
                .thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> appService.queryCurrentRelation(ACCOUNT, VIN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db down");
        verify(userVehicleRelationRepository, never()).insert(any());
        verify(userVehicleAuditRepository, never()).insert(any());
    }

    // ================= 协议同意证据（CMS 契约对齐） =================

    private AgreementBundle bundle() {
        return new AgreementBundle("B-1", "zh-CN", java.util.List.of(
                new AgreementContent("PRIVACY_POLICY", "1.0", "REV-P", "zh-CN", "隐私政策", "sha256:p", "<p/>"),
                new AgreementContent("USER_AGREEMENT", "1.0", "REV-U", "zh-CN", "用户协议", "sha256:u", "<p/>")));
    }

    private java.util.List<AgreementAcceptanceRequest> acceptances(String privacyHash, String userHash) {
        return java.util.List.of(
                AgreementAcceptanceRequest.builder().agreementType(AgreementType.PRIVACY_POLICY).agreed(true)
                        .version("1.0").revisionId("REV-P").language("zh-CN").contentHash(privacyHash).build(),
                AgreementAcceptanceRequest.builder().agreementType(AgreementType.USER_AGREEMENT).agreed(true)
                        .version("1.0").revisionId("REV-U").language("zh-CN").contentHash(userHash).build());
    }

    @Test
    @DisplayName("协议内容摘要与 CMS 已发布内容不一致：拒绝签发会话")
    void createSessionRejectsContentHashMismatch() {
        when(agreementContentPort.loadCurrent(anyString())).thenReturn(bundle());

        assertThatThrownBy(() -> appService.createSession("device-ref", VIN,
                acceptances("sha256:tampered", "sha256:tampered"), null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.AGREEMENT_NOT_ACCEPTED.getCode()));
        verify(activationSessionRepository, never()).insert(any());
    }

    @Test
    @DisplayName("协议修订与内容摘要均匹配：签发会话并记录展示修订")
    void createSessionAcceptsMatchingEvidence() {
        when(agreementContentPort.loadCurrent(anyString())).thenReturn(bundle());
        doAnswer(invocation -> {
            ActivationSession session = invocation.getArgument(0);
            session.setId(1L);
            return null;
        }).when(activationSessionRepository).insert(any(ActivationSession.class));

        var result = appService.createSession("device-ref", VIN, acceptances("sha256:p", "sha256:u"), null);

        assertThat(result.getSessionId()).isNotBlank();
        assertThat(result.getQrToken()).isNotBlank();
        verify(activationSessionRepository).insert(any(ActivationSession.class));
    }

    // ================= 会话创建幂等台账（IOV-TSP-DSN-CR-020 D020-04/D020-05） =================

    private ActivationSession issuedSession() {
        return ActivationSession.builder()
                .sessionId(SESSION_ID)
                .vin(VIN)
                .state(ActivationSessionState.ISSUED)
                .expiresAt(NOW.plusSeconds(600))
                .rowVersion(1)
                .build();
    }

    @Test
    @DisplayName("幂等回放：同键同体不新建会话，sessionId/expiresAt 复用且二维码重签（旧码失效）")
    void createSessionIdempotentReplayResignsQrToken() {
        when(agreementContentPort.loadCurrent(anyString())).thenReturn(bundle());
        String digest = DigestUtils.sha256Hex("VEHICLE_SESSION|" + VIN + "|" + String.join(";", List.of(
                "PRIVACY_POLICY:REV-P:true:zh-CN:sha256:p",
                "USER_AGREEMENT:REV-U:true:zh-CN:sha256:u")));
        ActivationSessionIdempotency record = ActivationSessionIdempotency.builder()
                .id(9L).callerScope(BindingCallerScope.VEHICLE).vin(VIN).idempotencyKey(KEY)
                .requestDigest(digest).sessionId(SESSION_ID).expiresAt(NOW.plusSeconds(600)).build();
        when(activationSessionIdempotencyRepository.find(BindingCallerScope.VEHICLE, VIN, KEY))
                .thenReturn(Optional.of(record));
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(issuedSession()));
        when(activationSessionRepository.resignQrToken(eq(SESSION_ID), anyString(), eq(NOW), eq(1)))
                .thenReturn(true);

        var result = appService.createSession("device-ref", VIN, acceptances("sha256:p", "sha256:u"), KEY);

        assertThat(result.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(result.getExpiresAt()).isEqualTo(NOW.plusSeconds(600));
        assertThat(result.getQrToken()).isNotBlank();
        // 不新建会话、不新建台账/二维码孤儿
        verify(activationSessionRepository, never()).insert(any());
        verify(activationSessionIdempotencyRepository, never()).insert(any());
        verify(activationSessionRepository).resignQrToken(eq(SESSION_ID), anyString(), eq(NOW), eq(1));
    }

    @Test
    @DisplayName("幂等同键异体：返回 401110，不新建会话")
    void createSessionIdempotentConflict() {
        when(agreementContentPort.loadCurrent(anyString())).thenReturn(bundle());
        when(activationSessionIdempotencyRepository.find(BindingCallerScope.VEHICLE, VIN, KEY))
                .thenReturn(Optional.of(ActivationSessionIdempotency.builder()
                        .id(9L).callerScope(BindingCallerScope.VEHICLE).vin(VIN).idempotencyKey(KEY)
                        .requestDigest("different-digest").sessionId(SESSION_ID)
                        .expiresAt(NOW.plusSeconds(600)).build()));

        assertThatThrownBy(() -> appService.createSession("device-ref", VIN,
                acceptances("sha256:p", "sha256:u"), KEY))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.IDEMPOTENCY_CONFLICT.getCode()));
        verify(activationSessionRepository, never()).insert(any());
    }

    @Test
    @DisplayName("幂等台账过期：释放后按新建处理（同一幂等键可复用）")
    void createSessionIdempotentExpiredReleased() {
        when(agreementContentPort.loadCurrent(anyString())).thenReturn(bundle());
        when(activationSessionIdempotencyRepository.find(BindingCallerScope.VEHICLE, VIN, KEY))
                .thenReturn(Optional.of(ActivationSessionIdempotency.builder()
                        .id(9L).callerScope(BindingCallerScope.VEHICLE).vin(VIN).idempotencyKey(KEY)
                        .requestDigest("any").sessionId(SESSION_ID)
                        .expiresAt(NOW.minusSeconds(1)).build()));

        var result = appService.createSession("device-ref", VIN, acceptances("sha256:p", "sha256:u"), KEY);

        assertThat(result.getSessionId()).isNotBlank();
        verify(activationSessionIdempotencyRepository).deleteById(9L);
        verify(activationSessionRepository).insert(any(ActivationSession.class));
        verify(activationSessionIdempotencyRepository).insert(any(ActivationSessionIdempotency.class));
    }

    @Test
    @DisplayName("幂等回放：会话已非 ISSUED（已扫码）→ 401101 且不重签")
    void createSessionReplayRejectsNonIssuedSession() {
        when(agreementContentPort.loadCurrent(anyString())).thenReturn(bundle());
        String digest = DigestUtils.sha256Hex("VEHICLE_SESSION|" + VIN + "|" + String.join(";", List.of(
                "PRIVACY_POLICY:REV-P:true:zh-CN:sha256:p",
                "USER_AGREEMENT:REV-U:true:zh-CN:sha256:u")));
        when(activationSessionIdempotencyRepository.find(BindingCallerScope.VEHICLE, VIN, KEY))
                .thenReturn(Optional.of(ActivationSessionIdempotency.builder()
                        .id(9L).callerScope(BindingCallerScope.VEHICLE).vin(VIN).idempotencyKey(KEY)
                        .requestDigest(digest).sessionId(SESSION_ID)
                        .expiresAt(NOW.plusSeconds(600)).build()));
        ActivationSession scanned = issuedSession();
        scanned.setState(ActivationSessionState.SCANNED);
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(scanned));

        assertThatThrownBy(() -> appService.createSession("device-ref", VIN,
                acceptances("sha256:p", "sha256:u"), KEY))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.SESSION_INVALID.getCode()));
        verify(activationSessionRepository, never()).resignQrToken(anyString(), anyString(), any(), anyInt());
    }

    @Test
    @DisplayName("幂等键格式非法：401112 且不查台账、不新建会话")
    void createSessionRejectsInvalidIdempotencyKey() {
        when(agreementContentPort.loadCurrent(anyString())).thenReturn(bundle());

        assertThatThrownBy(() -> appService.createSession("device-ref", VIN,
                acceptances("sha256:p", "sha256:u"), "bad key with space"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.INVALID_REQUEST.getCode()));
        verify(activationSessionIdempotencyRepository, never()).find(any(), anyString(), anyString());
        verify(activationSessionRepository, never()).insert(any());
    }

    // ================= 绑定结果口径（IOV-TSP-DSN-CR-020 D020-08 / R-6） =================

    @Test
    @DisplayName("结果查询：relationId 优先取绑定操作 resultRelationId，来源为 BINDING_OPERATION")
    void queryVehicleResultPrefersBindingOperationRelation() {
        ActivationSession bound = issuedSession();
        bound.setState(ActivationSessionState.BOUND);
        bound.setBindingOperationId("BOP-1");
        bound.setResultExpiresAt(NOW.plusSeconds(1800));
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(bound));
        when(bindingOperationRepository.findByOperationId("BOP-1")).thenReturn(Optional.of(
                BindingOperation.builder().operationId("BOP-1").status(BindingOperationStatus.SUCCEEDED)
                        .resultRelationId("REL-BOUND").build()));

        var result = appService.queryVehicleResult(VIN, SESSION_ID);

        assertThat(result.getRelationId()).isEqualTo("REL-BOUND");
        assertThat(result.getRelationSource()).isEqualTo(RelationSource.BINDING_OPERATION);
        verify(userVehicleRelationRepository, never()).findActiveByVin(anyString());
    }

    @Test
    @DisplayName("结果查询：绑定操作缺失时回退当前有效关系，来源为 ACTIVE_RELATION")
    void queryVehicleResultFallsBackToActiveRelation() {
        ActivationSession bound = issuedSession();
        bound.setState(ActivationSessionState.BOUND);
        bound.setBindingOperationId("BOP-1");
        bound.setResultExpiresAt(NOW.plusSeconds(1800));
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(bound));
        when(bindingOperationRepository.findByOperationId("BOP-1")).thenReturn(Optional.empty());
        when(userVehicleRelationRepository.findActiveByVin(VIN)).thenReturn(Optional.of(
                UserVehicleRelation.builder().relationId("REL-ACTIVE").accountId(ACCOUNT).vin(VIN).build()));

        var result = appService.queryVehicleResult(VIN, SESSION_ID);

        assertThat(result.getRelationId()).isEqualTo("REL-ACTIVE");
        assertThat(result.getRelationSource()).isEqualTo(RelationSource.ACTIVE_RELATION);
    }

    @Test
    @DisplayName("结果查询：relationId 最终为空时来源固定为 ACTIVE_RELATION")
    void queryVehicleResultEmptyRelationKeepsActiveSource() {
        ActivationSession bound = issuedSession();
        bound.setState(ActivationSessionState.BOUND);
        bound.setBindingOperationId("BOP-1");
        bound.setResultExpiresAt(NOW.plusSeconds(1800));
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(bound));
        when(bindingOperationRepository.findByOperationId("BOP-1")).thenReturn(Optional.empty());
        when(userVehicleRelationRepository.findActiveByVin(VIN)).thenReturn(Optional.empty());

        var result = appService.queryVehicleResult(VIN, SESSION_ID);

        assertThat(result.getRelationId()).isNull();
        assertThat(result.getRelationSource()).isEqualTo(RelationSource.ACTIVE_RELATION);
    }
}
