package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ActivationSessionState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingCallerScope;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingOperationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingResultReason;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.ConfirmVehicleBindingRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ConfirmVehicleBindingVo;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.UserVehicleBindingErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.DigestUtils;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationSession;
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
                activationSessionRepository, userVehicleRelationRepository, bindingOperationRepository,
                activationFactRepository, userVehicleAuditRepository, new BindingEligibilityPolicy(), properties,
                new TransactionTemplate(transactionManager), Clock.fixed(NOW, ZoneOffset.UTC));
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
}
