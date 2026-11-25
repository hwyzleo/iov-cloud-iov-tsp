package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CredentialRevocationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UnbindRelationState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.MptUserVehicleUnbindRequest;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUserVehicleUnbindResult;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.UserVehicleBindingErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.DigestUtils;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.BindingOperation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.CredentialRevocationResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleRelation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleUnbindOperation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.CredentialRevocationPort;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.DependencyUnavailableException;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationLoginTicketRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.BindingOperationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleAuditRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleRelationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleUnbindOperationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
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
 * 人工解绑应用服务测试（IOV-TSP-DSN-CR-021 TC021-04～TC021-12）。
 * <p>
 * 覆盖 CAS/幂等/票据作废原子性、CIAM 成功/超时/失败的真实状态表达、
 * 自动与人工重试边界，以及关系不回滚语义。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserVehicleUnbindAppServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-25T10:00:00Z");
    private static final String OPERATOR = "mpt-admin";
    private static final String RELATION_ID = "REL-1";
    private static final String VIN = "LSVAA1234567890AB";
    private static final String ACCOUNT = "account-1";
    private static final String SESSION_ID = "SES-1";
    private static final String KEY = "unbind-key-1";
    private static final String REASON = "CUSTOMER_VERIFIED_REQUEST";
    private static final int ROW_VERSION = 3;

    @Mock
    private UserVehicleRelationRepository userVehicleRelationRepository;
    @Mock
    private UserVehicleUnbindOperationRepository userVehicleUnbindOperationRepository;
    @Mock
    private BindingOperationRepository bindingOperationRepository;
    @Mock
    private ActivationLoginTicketRepository activationLoginTicketRepository;
    @Mock
    private UserVehicleAuditRepository userVehicleAuditRepository;
    @Mock
    private CredentialRevocationPort credentialRevocationPort;
    @Mock
    private PlatformTransactionManager transactionManager;

    private TspUserVehicleProperties properties;
    private UserVehicleUnbindAppService appService;
    private final AtomicReference<UserVehicleUnbindOperation> insertedOperation = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        properties = new TspUserVehicleProperties();
        properties.getUnbind().setRevokeTimeout(Duration.ofSeconds(2));
        properties.getUnbind().setRetryBackoff(Duration.ofSeconds(30));
        properties.getUnbind().setRetryMaxAttempts(10);
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        appService = new UserVehicleUnbindAppService(userVehicleRelationRepository,
                userVehicleUnbindOperationRepository, bindingOperationRepository, activationLoginTicketRepository,
                userVehicleAuditRepository, credentialRevocationPort, properties,
                new TransactionTemplate(transactionManager), Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().doAnswer(invocation -> {
            UserVehicleUnbindOperation operation = invocation.getArgument(0);
            operation.setId(1L);
            insertedOperation.set(operation);
            return null;
        }).when(userVehicleUnbindOperationRepository).insert(any(UserVehicleUnbindOperation.class));
        lenient().when(userVehicleRelationRepository.findByRelationId(RELATION_ID))
                .thenReturn(Optional.of(relation("ACTIVE", ROW_VERSION)));
        lenient().when(userVehicleRelationRepository.lockByRelationId(RELATION_ID))
                .thenReturn(Optional.of(relation("ACTIVE", ROW_VERSION)));
        lenient().when(userVehicleRelationRepository.release(eq(RELATION_ID), eq(ROW_VERSION), any(), anyString(),
                anyString(), eq(OPERATOR), any())).thenReturn(true);
        lenient().when(bindingOperationRepository.findByOperationId("BOP-1"))
                .thenReturn(Optional.of(BindingOperation.builder().operationId("BOP-1")
                        .sessionId(SESSION_ID).resultRelationId(RELATION_ID).build()));
        lenient().when(userVehicleUnbindOperationRepository.markReleased(anyString(), any(), any(), any(), any(),
                eq(1))).thenReturn(true);
        lenient().when(userVehicleUnbindOperationRepository.markRevocationSucceeded(anyString(), anyInt(), any(),
                eq(2))).thenReturn(true);
        lenient().when(userVehicleUnbindOperationRepository.markRevocationFailed(anyString(), anyString(), any(),
                eq(true), any(), eq(2))).thenReturn(true);
    }

    private UserVehicleRelation relation(String state, int rowVersion) {
        return UserVehicleRelation.builder()
                .relationId(RELATION_ID)
                .accountId(ACCOUNT)
                .vin(VIN)
                .role("PRIMARY_USER")
                .state(state)
                .boundAt(NOW.minusSeconds(3600))
                .sourceOperationId("BOP-1")
                .rowVersion(rowVersion)
                .build();
    }

    private MptUserVehicleUnbindRequest request() {
        return MptUserVehicleUnbindRequest.builder()
                .rowVersion(ROW_VERSION).reason(REASON).idempotencyKey(KEY).build();
    }

    private UserVehicleUnbindOperation releasedOperation(CredentialRevocationStatus status, Integer count) {
        return UserVehicleUnbindOperation.builder()
                .operationId("UBO-1").relationId(RELATION_ID).accountId(ACCOUNT).vin(VIN)
                .operatorId(OPERATOR).idempotencyKey(KEY)
                .requestDigest(digest())
                .expectedRowVersion(ROW_VERSION).reason(REASON)
                .relationState(UnbindRelationState.RELEASED)
                .releasedAt(NOW).completedAt(NOW)
                .credentialRevocationStatus(status)
                .revokedSessionCount(count)
                .retryCount(0).rowVersion(2)
                .build();
    }

    private String digest() {
        return DigestUtils.sha256Hex(String.join("|", "UNBIND", OPERATOR, RELATION_ID,
                String.valueOf(ROW_VERSION), REASON));
    }

    @Test
    @DisplayName("正常解绑：关系 RELEASED + 台账 + 审计 + 票据作废，CIAM 成功后撤销 SUCCEEDED")
    void unbindSuccess() {
        when(credentialRevocationPort.revoke(anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(new CredentialRevocationResult("UBO-1", RELATION_ID, 2));
        when(userVehicleUnbindOperationRepository.findByOperationId(anyString()))
                .thenReturn(Optional.of(releasedOperation(CredentialRevocationStatus.SUCCEEDED, 2)));

        MptUserVehicleUnbindResult result = appService.unbind(OPERATOR, RELATION_ID, request());

        assertThat(result.getRelationState()).isEqualTo(UnbindRelationState.RELEASED);
        assertThat(result.getCredentialRevocationStatus()).isEqualTo(CredentialRevocationStatus.SUCCEEDED);
        assertThat(result.getRevokedSessionCount()).isEqualTo(2);
        verify(userVehicleRelationRepository).release(eq(RELATION_ID), eq(ROW_VERSION), eq(NOW), eq(REASON),
                anyString(), eq(OPERATOR), eq(NOW));
        verify(activationLoginTicketRepository).revokeIssuedBySession(SESSION_ID, NOW);
        verify(userVehicleAuditRepository, org.mockito.Mockito.atLeastOnce()).insert(any());
        ArgumentCaptor<UserVehicleUnbindOperation> captor =
                ArgumentCaptor.forClass(UserVehicleUnbindOperation.class);
        verify(userVehicleUnbindOperationRepository).insert(captor.capture());
        // 同一实例在后续状态推进中会被修改：此处只断言认领时固化的请求事实
        assertThat(captor.getValue().getExpectedRowVersion()).isEqualTo(ROW_VERSION);
        assertThat(captor.getValue().getRequestDigest()).isEqualTo(digest());
        verify(userVehicleUnbindOperationRepository).markReleased(anyString(), any(), any(), any(), any(), eq(1));
    }

    @Test
    @DisplayName("同操作人同键同体：返回既有结果，不重复释放、不再调 CIAM")
    void replaySameKeySameBody() {
        when(userVehicleUnbindOperationRepository.findByOperatorAndKey(OPERATOR, KEY))
                .thenReturn(Optional.of(releasedOperation(CredentialRevocationStatus.SUCCEEDED, 1)));

        MptUserVehicleUnbindResult result = appService.unbind(OPERATOR, RELATION_ID, request());

        assertThat(result.getCredentialRevocationStatus()).isEqualTo(CredentialRevocationStatus.SUCCEEDED);
        verify(userVehicleRelationRepository, never()).release(anyString(), anyInt(), any(), anyString(), anyString(),
                anyString(), any());
        verify(credentialRevocationPort, never()).revoke(anyString(), anyString(), anyString(), anyString(), any(),
                anyString());
    }

    @Test
    @DisplayName("同操作人同键异体：401110")
    void replaySameKeyDifferentBody() {
        UserVehicleUnbindOperation existing = releasedOperation(CredentialRevocationStatus.SUCCEEDED, 1);
        existing.setRequestDigest("another-digest");
        when(userVehicleUnbindOperationRepository.findByOperatorAndKey(OPERATOR, KEY))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> appService.unbind(OPERATOR, RELATION_ID, request()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.IDEMPOTENCY_CONFLICT.getCode()));
    }

    @Test
    @DisplayName("relationId 不存在：401120")
    void relationNotFound() {
        when(userVehicleRelationRepository.findByRelationId(RELATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appService.unbind(OPERATOR, RELATION_ID, request()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.USER_VEHICLE_RELATION_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("关系已 RELEASED：401121，不释放、不作废票据")
    void relationAlreadyReleased() {
        when(userVehicleRelationRepository.lockByRelationId(RELATION_ID))
                .thenReturn(Optional.of(relation("RELEASED", ROW_VERSION + 1)));

        assertThatThrownBy(() -> appService.unbind(OPERATOR, RELATION_ID, request()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.USER_VEHICLE_RELATION_STATE_CONFLICT.getCode()));
        verify(userVehicleRelationRepository, never()).release(anyString(), anyInt(), any(), anyString(), anyString(),
                anyString(), any());
        verify(activationLoginTicketRepository, never()).revokeIssuedBySession(anyString(), any());
    }

    @Test
    @DisplayName("预期 rowVersion 过期：401121")
    void rowVersionConflict() {
        when(userVehicleRelationRepository.lockByRelationId(RELATION_ID))
                .thenReturn(Optional.of(relation("ACTIVE", ROW_VERSION + 1)));

        assertThatThrownBy(() -> appService.unbind(OPERATOR, RELATION_ID, request()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.USER_VEHICLE_RELATION_STATE_CONFLICT.getCode()));
    }

    @Test
    @DisplayName("条件释放影响 0 行（并发）：401121")
    void releaseCasConflict() {
        when(userVehicleRelationRepository.release(eq(RELATION_ID), eq(ROW_VERSION), any(), anyString(), anyString(),
                anyString(), any())).thenReturn(false);

        assertThatThrownBy(() -> appService.unbind(OPERATOR, RELATION_ID, request()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.USER_VEHICLE_RELATION_STATE_CONFLICT.getCode()));
    }

    @Test
    @DisplayName("票据作废失败：本地事务失败，关系不提交")
    void ticketRevocationFailureAbortsRelease() {
        when(activationLoginTicketRepository.revokeIssuedBySession(SESSION_ID, NOW))
                .thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> appService.unbind(OPERATOR, RELATION_ID, request()))
                .isInstanceOf(IllegalStateException.class);
        verify(userVehicleUnbindOperationRepository, never()).markReleased(anyString(), any(), any(), any(), any(),
                anyInt());
    }

    @Test
    @DisplayName("CIAM 超时/失败：关系保持 RELEASED，撤销 FAILED，结果真实不回滚")
    void ciamFailureKeepsReleasedRelation() {
        when(credentialRevocationPort.revoke(anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenThrow(new DependencyUnavailableException("timeout"));
        when(userVehicleUnbindOperationRepository.findByOperationId(anyString()))
                .thenReturn(Optional.of(releasedOperation(CredentialRevocationStatus.FAILED, null)));

        MptUserVehicleUnbindResult result = appService.unbind(OPERATOR, RELATION_ID, request());

        assertThat(result.getRelationState()).isEqualTo(UnbindRelationState.RELEASED);
        assertThat(result.getCredentialRevocationStatus()).isEqualTo(CredentialRevocationStatus.FAILED);
        verify(userVehicleUnbindOperationRepository).markRevocationFailed(anyString(), anyString(), any(), eq(true),
                any(), eq(2));
        verify(userVehicleUnbindOperationRepository, never()).markRevocationSucceeded(anyString(), anyInt(), any(),
                anyInt());
    }

    @Test
    @DisplayName("CIAM 成功但台账写入失败：不抛异常，返回 PENDING 等待按原 operationId 重试")
    void ciamSuccessButLedgerFailureKeepsPending() {
        when(credentialRevocationPort.revoke(anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(new CredentialRevocationResult("UBO-1", RELATION_ID, 1));
        when(userVehicleUnbindOperationRepository.markRevocationSucceeded(anyString(), anyInt(), any(), eq(2)))
                .thenThrow(new IllegalStateException("db down"));
        when(userVehicleUnbindOperationRepository.findByOperationId(anyString()))
                .thenReturn(Optional.of(releasedOperation(CredentialRevocationStatus.PENDING, null)));

        MptUserVehicleUnbindResult result = appService.unbind(OPERATOR, RELATION_ID, request());

        assertThat(result.getRelationState()).isEqualTo(UnbindRelationState.RELEASED);
        assertThat(result.getCredentialRevocationStatus()).isEqualTo(CredentialRevocationStatus.PENDING);
    }

    @Test
    @DisplayName("人工重试：仅 RELEASED 且未 SUCCEEDED；沿用原 operationId")
    void manualRetryReusesOperation() {
        UserVehicleUnbindOperation failed = releasedOperation(CredentialRevocationStatus.FAILED, null);
        when(userVehicleUnbindOperationRepository.findByOperationId("UBO-1"))
                .thenReturn(Optional.of(failed))
                .thenReturn(Optional.of(releasedOperation(CredentialRevocationStatus.SUCCEEDED, 1)));
        when(credentialRevocationPort.revoke(eq("UBO-1"), eq(RELATION_ID), eq(ACCOUNT), eq(VIN), eq(NOW), eq(REASON)))
                .thenReturn(new CredentialRevocationResult("UBO-1", RELATION_ID, 1));

        MptUserVehicleUnbindResult result = appService.retryRevocation(OPERATOR, "UBO-1");

        assertThat(result.getCredentialRevocationStatus()).isEqualTo(CredentialRevocationStatus.SUCCEEDED);
        verify(userVehicleRelationRepository, never()).release(anyString(), anyInt(), any(), anyString(), anyString(),
                anyString(), any());
    }

    @Test
    @DisplayName("人工重试：关系未 RELEASED 拒绝 401121")
    void manualRetryRejectsUnreleasedRelation() {
        UserVehicleUnbindOperation pending = releasedOperation(CredentialRevocationStatus.PENDING, null);
        pending.setRelationState(UnbindRelationState.PENDING);
        when(userVehicleUnbindOperationRepository.findByOperationId("UBO-1")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> appService.retryRevocation(OPERATOR, "UBO-1"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.USER_VEHICLE_RELATION_STATE_CONFLICT.getCode()));
    }

    @Test
    @DisplayName("撤销已 SUCCEEDED：重试为幂等空操作")
    void manualRetrySucceededIsNoop() {
        when(userVehicleUnbindOperationRepository.findByOperationId("UBO-1"))
                .thenReturn(Optional.of(releasedOperation(CredentialRevocationStatus.SUCCEEDED, 3)));

        MptUserVehicleUnbindResult result = appService.retryRevocation(OPERATOR, "UBO-1");

        assertThat(result.getCredentialRevocationStatus()).isEqualTo(CredentialRevocationStatus.SUCCEEDED);
        verify(credentialRevocationPort, never()).revoke(anyString(), anyString(), anyString(), anyString(), any(),
                anyString());
    }

    @Test
    @DisplayName("自动重试：按持久化候选逐个收敛，达到上限后不再进入候选")
    void autoRetryCandidates() {
        UserVehicleUnbindOperation failed = releasedOperation(CredentialRevocationStatus.FAILED, null);
        failed.setNextRetryAt(NOW.minusSeconds(1));
        when(userVehicleUnbindOperationRepository.findRevocationRetryCandidates(NOW, 10, 20))
                .thenReturn(List.of(failed));
        when(credentialRevocationPort.revoke(anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(new CredentialRevocationResult("UBO-1", RELATION_ID, 1));
        when(userVehicleUnbindOperationRepository.findByOperationId("UBO-1"))
                .thenReturn(Optional.of(releasedOperation(CredentialRevocationStatus.SUCCEEDED, 1)));

        appService.retryPendingRevocations();

        verify(credentialRevocationPort).revoke(eq("UBO-1"), eq(RELATION_ID), eq(ACCOUNT), eq(VIN), any(), any());
        verify(userVehicleUnbindOperationRepository).markRevocationSucceeded("UBO-1", 1, NOW, 2);
    }

    @Test
    @DisplayName("回滚开关：unbind.enabled=false 时拒绝写入")
    void disabledRejectsUnbind() {
        properties.getUnbind().setEnabled(false);
        UserVehicleUnbindAppService disabled = new UserVehicleUnbindAppService(userVehicleRelationRepository,
                userVehicleUnbindOperationRepository, bindingOperationRepository, activationLoginTicketRepository,
                userVehicleAuditRepository, credentialRevocationPort, properties,
                new TransactionTemplate(transactionManager), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> disabled.unbind(OPERATOR, RELATION_ID, request()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.INVALID_REQUEST.getCode()));
    }
}
