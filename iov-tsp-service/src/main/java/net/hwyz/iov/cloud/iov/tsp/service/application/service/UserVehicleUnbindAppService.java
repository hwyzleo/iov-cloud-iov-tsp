package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CredentialRevocationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UnbindRelationState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.MptUserVehicleUnbindRequest;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUserVehicleUnbindResult;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.UserVehicleBindingErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.DigestUtils;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.BindingOperation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.CredentialRevocationResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleAuditRecord;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleRelation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleUnbindOperation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.CredentialRevocationPort;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationLoginTicketRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.BindingOperationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleAuditRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleRelationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleUnbindOperationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * MPT 用户车辆关系人工解绑应用服务（IOV-TSP-DSN-CR-021 D021-03/D021-04/D021-06/D021-07/D021-09）。
 * <p>
 * 事务边界：
 * <ol>
 *   <li>幂等操作认领（唯一键 operator+idempotencyKey）→ relation 行锁与 ACTIVE/rowVersion 重检 →
 *       条件 RELEASED + 释放四列 → 台账 RELEASED → 审计 → 来源会话未消费票据 REVOKED，同一本地事务；</li>
 *   <li>CIAM 撤销调用固定在本地事务提交之后、行锁之外；失败/超时/响应非法不改变已释放关系，
 *       只把台账撤销状态推进为 PENDING/FAILED 并按原 operationId 重试；</li>
 *   <li>票据作废失败视为本地事务失败，关系不提交，避免「关系已释放但旧票据仍可消费」。</li>
 * </ol>
 * 关系释放与凭证撤销是两个独立事实；成功码只表示关系状态已权威确定，
 * {@code RELEASED + PENDING/FAILED} 不是回滚，也不是全部成功。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserVehicleUnbindAppService {

    private static final String OPERATOR_SOURCE_MPT = "MPT";
    private static final String BIZ_UNBIND = "UNBIND";
    private static final String BIZ_REVOCATION = "CREDENTIAL_REVOCATION";
    private static final String STATE_ACTIVE = "ACTIVE";
    private static final String STATE_RELEASED = "RELEASED";

    private final UserVehicleRelationRepository userVehicleRelationRepository;
    private final UserVehicleUnbindOperationRepository userVehicleUnbindOperationRepository;
    private final BindingOperationRepository bindingOperationRepository;
    private final ActivationLoginTicketRepository activationLoginTicketRepository;
    private final UserVehicleAuditRepository userVehicleAuditRepository;
    private final CredentialRevocationPort credentialRevocationPort;
    private final TspUserVehicleProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    /**
     * 人工解绑指定关系（幂等 + 原子释放 + 事务外 CIAM 撤销）
     *
     * @param operatorId 可信 MPT 操作人
     * @param relationId 关系ID
     * @param request    解绑请求
     * @return 关系与凭证撤销两个独立状态
     */
    public MptUserVehicleUnbindResult unbind(String operatorId, String relationId,
                                             MptUserVehicleUnbindRequest request) {
        requireEnabled();
        String key = trim(request.getIdempotencyKey());
        String reason = trim(request.getReason());
        int expectedRowVersion = request.getRowVersion();
        String digest = requestDigest(operatorId, relationId, expectedRowVersion, reason);

        Optional<UserVehicleUnbindOperation> existing =
                userVehicleUnbindOperationRepository.findByOperatorAndKey(operatorId, key);
        if (existing.isPresent()) {
            return replay(existing.get(), digest);
        }

        UserVehicleUnbindOperation operation;
        try {
            operation = transactionTemplate.execute(status ->
                    createAndRelease(operatorId, relationId, expectedRowVersion, reason, key, digest));
        } catch (DuplicateKeyException e) {
            // 并发同键：重新按台账判定，不重复迁移关系
            UserVehicleUnbindOperation concurrent = userVehicleUnbindOperationRepository
                    .findByOperatorAndKey(operatorId, key)
                    .orElseThrow(() -> business(UserVehicleBindingErrorCode.IDEMPOTENCY_CONFLICT));
            return replay(concurrent, digest);
        }
        if (operation == null) {
            throw business(UserVehicleBindingErrorCode.INVALID_REQUEST);
        }
        // 本地关系已提交；CIAM RPC 在事务与行锁之外执行
        UserVehicleUnbindOperation converged = attemptRevocation(operation);
        return toUnbindResult(converged);
    }

    /**
     * 人工重试凭证撤销（沿用原 operationId，不再次迁移关系）
     *
     * @param operatorId  可信 MPT 操作人
     * @param operationId 解绑操作ID
     * @return 最新权威结果
     */
    public MptUserVehicleUnbindResult retryRevocation(String operatorId, String operationId) {
        requireEnabled();
        UserVehicleUnbindOperation operation = userVehicleUnbindOperationRepository
                .findByOperationId(operationId)
                .orElseThrow(() -> business(UserVehicleBindingErrorCode.INVALID_REQUEST));
        if (operation.getRelationState() != UnbindRelationState.RELEASED) {
            throw business(UserVehicleBindingErrorCode.USER_VEHICLE_RELATION_STATE_CONFLICT);
        }
        if (operation.getCredentialRevocationStatus() == CredentialRevocationStatus.SUCCEEDED) {
            // 已收敛：重试为幂等空操作，不再调用 CIAM
            return toUnbindResult(operation);
        }
        try {
            audit(BIZ_REVOCATION, operation.getOperationId(), operation.getVin(), operation.getAccountId(),
                    operation.getCredentialRevocationStatus().name(), "MANUAL_RETRY", OPERATOR_SOURCE_MPT,
                    "MANUAL_RETRY", clock.instant());
        } catch (RuntimeException e) {
            // 审计暂不可写不应阻断人工重试；关系已释放与撤销状态仍以台账为准
            log.warn("人工重试审计写入失败: operationId={}, error={}", operationId, e.getClass().getSimpleName());
        }
        // 人工重试不受自动重试次数上限约束，但仍沿用原 operationId 的 CIAM 幂等
        UserVehicleUnbindOperation converged = attemptRevocation(operation);
        return toUnbindResult(converged);
    }

    /**
     * 自动重试待收敛的撤销操作（由定时任务调用，不抛业务异常）
     */
    public void retryPendingRevocations() {
        TspUserVehicleProperties.Unbind config = properties.getUnbind();
        if (!config.isEnabled()) {
            return;
        }
        Instant now = clock.instant();
        List<UserVehicleUnbindOperation> candidates = userVehicleUnbindOperationRepository
                .findRevocationRetryCandidates(now, config.getRetryMaxAttempts(), config.getRetryBatchSize());
        for (UserVehicleUnbindOperation operation : candidates) {
            try {
                attemptRevocation(operation);
            } catch (Exception e) {
                log.warn("自动重试凭证撤销失败: operationId={}, error={}", operation.getOperationId(),
                        e.getClass().getSimpleName());
            }
        }
    }

    // ================= 内部实现 =================

    /**
     * 同一本地事务内认领操作、释放关系、作废票据并写审计
     */
    private UserVehicleUnbindOperation createAndRelease(String operatorId, String relationId, int expectedRowVersion,
                                                        String reason, String key, String digest) {
        UserVehicleRelation snapshot = userVehicleRelationRepository.findByRelationId(relationId)
                .orElseThrow(() -> business(UserVehicleBindingErrorCode.USER_VEHICLE_RELATION_NOT_FOUND));
        Instant now = clock.instant();
        UserVehicleUnbindOperation operation = UserVehicleUnbindOperation.builder()
                .operationId(newId("UBO"))
                .relationId(relationId)
                .accountId(snapshot.getAccountId())
                .vin(snapshot.getVin())
                .operatorId(operatorId)
                .idempotencyKey(key)
                .requestDigest(digest)
                .expectedRowVersion(expectedRowVersion)
                .reason(reason)
                .relationState(UnbindRelationState.PENDING)
                .credentialRevocationStatus(CredentialRevocationStatus.PENDING)
                .retryCount(0)
                .rowVersion(1)
                .build();
        userVehicleUnbindOperationRepository.insert(operation);

        UserVehicleRelation locked = userVehicleRelationRepository.lockByRelationId(relationId)
                .orElseThrow(() -> business(UserVehicleBindingErrorCode.USER_VEHICLE_RELATION_NOT_FOUND));
        validateReleasable(locked, operation, expectedRowVersion);

        String sessionId = resolveSourceSessionId(locked.getSourceOperationId());
        if (!userVehicleRelationRepository.release(relationId, expectedRowVersion, now, reason,
                operation.getOperationId(), operatorId, now)) {
            throw business(UserVehicleBindingErrorCode.USER_VEHICLE_RELATION_STATE_CONFLICT);
        }
        if (sessionId != null) {
            // D021-04：票据作废失败属于本地事务失败，关系不提交
            activationLoginTicketRepository.revokeIssuedBySession(sessionId, now);
        }
        Instant nextRetryAt = now.plus(properties.getUnbind().getRetryBackoff());
        if (!userVehicleUnbindOperationRepository.markReleased(operation.getOperationId(), now, now, nextRetryAt,
                now, 1)) {
            throw business(UserVehicleBindingErrorCode.USER_VEHICLE_RELATION_STATE_CONFLICT);
        }
        audit(BIZ_UNBIND, operation.getOperationId(), locked.getVin(), locked.getAccountId(), STATE_ACTIVE,
                STATE_RELEASED, OPERATOR_SOURCE_MPT, reason, now);
        operation.setRelationState(UnbindRelationState.RELEASED);
        operation.setReleasedAt(now);
        operation.setCompletedAt(now);
        operation.setNextRetryAt(nextRetryAt);
        operation.setRowVersion(2);
        log.info("人工解绑关系已释放: operationId={}, relationId={}, vin={}, operator={}",
                operation.getOperationId(), relationId, maskVin(locked.getVin()), operatorId);
        return operation;
    }

    private void validateReleasable(UserVehicleRelation locked, UserVehicleUnbindOperation operation,
                                    int expectedRowVersion) {
        if (!STATE_ACTIVE.equals(locked.getState())) {
            throw business(UserVehicleBindingErrorCode.USER_VEHICLE_RELATION_STATE_CONFLICT);
        }
        Integer currentVersion = locked.getRowVersion() == null ? 0 : locked.getRowVersion();
        if (currentVersion != expectedRowVersion) {
            throw business(UserVehicleBindingErrorCode.USER_VEHICLE_RELATION_STATE_CONFLICT);
        }
        // 锁定后重检 VIN/账号快照，防止操作对象在认领与加锁之间被替换
        if (!equalsText(locked.getVin(), operation.getVin())
                || !equalsText(locked.getAccountId(), operation.getAccountId())) {
            throw business(UserVehicleBindingErrorCode.USER_VEHICLE_RELATION_STATE_CONFLICT);
        }
    }

    /**
     * CIAM 撤销：调用在事务外；结果只推进台账，不回滚关系。
     * <p>
     * 本地关系已提交后，任何台账推进失败都不得把异常抛给调用方（否则用户看不到真实的 RELEASED 状态）：
     * 失败保留 PENDING/FAILED + next_retry_at，由自动/人工重试按原 operationId 收敛（CIAM 幂等）。
     */
    private UserVehicleUnbindOperation attemptRevocation(UserVehicleUnbindOperation operation) {
        String operationId = operation.getOperationId();
        Instant now = clock.instant();
        int expectedVersion = operation.getRowVersion() == null ? 2 : operation.getRowVersion();
        try {
            CredentialRevocationResult result = credentialRevocationPort.revoke(operationId,
                    operation.getRelationId(), operation.getAccountId(), operation.getVin(),
                    operation.getReleasedAt(), operation.getReason());
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    boolean updated = userVehicleUnbindOperationRepository.markRevocationSucceeded(operationId,
                            result.revokedSessionCount(), now, expectedVersion);
                    if (updated) {
                        audit(BIZ_REVOCATION, operationId, operation.getVin(), operation.getAccountId(),
                                CredentialRevocationStatus.PENDING.name(),
                                CredentialRevocationStatus.SUCCEEDED.name(),
                                OPERATOR_SOURCE_MPT, null, now);
                    }
                });
                log.info("CIAM 凭证撤销已收敛: operationId={}, revokedSessionCount={}", operationId,
                        result.revokedSessionCount());
            } catch (RuntimeException ledgerError) {
                // CIAM 已成功但 TSP 台账暂不可写：保持 PENDING，后续按原 operationId 重查/重试
                log.error("CIAM 撤销成功但台账更新失败，保留待重试: operationId={}, error={}", operationId,
                        ledgerError.getClass().getSimpleName());
            }
        } catch (Exception e) {
            int retryCount = operation.getRetryCount() == null ? 0 : operation.getRetryCount();
            int nextRetryCount = retryCount + 1;
            boolean exhausted = nextRetryCount >= properties.getUnbind().getRetryMaxAttempts();
            Instant nextRetryAt = exhausted ? null : now.plus(properties.getUnbind().getRetryBackoff());
            String error = truncate(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    boolean updated = userVehicleUnbindOperationRepository.markRevocationFailed(operationId, error,
                            nextRetryAt, true, now, expectedVersion);
                    if (updated) {
                        audit(BIZ_REVOCATION, operationId, operation.getVin(), operation.getAccountId(),
                                CredentialRevocationStatus.PENDING.name(),
                                CredentialRevocationStatus.FAILED.name(),
                                OPERATOR_SOURCE_MPT, "REVOCATION_FAILED", now);
                    }
                });
            } catch (RuntimeException ledgerError) {
                // 台账暂不可写：保持既有 PENDING + next_retry_at，交给自动重试
                log.error("撤销失败但台账更新失败，保留待重试: operationId={}, error={}", operationId,
                        ledgerError.getClass().getSimpleName());
            }
            log.warn("CIAM 凭证撤销未完成，保留台账重试: operationId={}, retryCount={}, exhausted={}",
                    operationId, nextRetryCount, exhausted);
        }
        try {
            return userVehicleUnbindOperationRepository.findByOperationId(operationId).orElse(operation);
        } catch (RuntimeException e) {
            log.error("读取解绑台账失败，返回本地已知状态: operationId={}, error={}", operationId,
                    e.getClass().getSimpleName());
            return operation;
        }
    }

    private MptUserVehicleUnbindResult replay(UserVehicleUnbindOperation operation, String requestDigest) {
        if (!requestDigest.equals(operation.getRequestDigest())) {
            throw business(UserVehicleBindingErrorCode.IDEMPOTENCY_CONFLICT);
        }
        return toUnbindResult(operation);
    }

    private MptUserVehicleUnbindResult toUnbindResult(UserVehicleUnbindOperation operation) {
        return MptUserVehicleUnbindResult.builder()
                .operationId(operation.getOperationId())
                .relationId(operation.getRelationId())
                .vin(operation.getVin())
                .relationState(operation.getRelationState())
                .credentialRevocationStatus(operation.getCredentialRevocationStatus())
                .releasedAt(operation.getReleasedAt())
                .revokedSessionCount(operation.getRevokedSessionCount())
                .build();
    }

    private String resolveSourceSessionId(String sourceOperationId) {
        if (isBlank(sourceOperationId)) {
            return null;
        }
        return bindingOperationRepository.findByOperationId(sourceOperationId)
                .map(BindingOperation::getSessionId)
                .orElse(null);
    }

    private String requestDigest(String operatorId, String relationId, int expectedRowVersion, String reason) {
        return DigestUtils.sha256Hex(String.join("|", "UNBIND", operatorId, relationId,
                String.valueOf(expectedRowVersion), reason));
    }

    private void requireEnabled() {
        if (!properties.getUnbind().isEnabled()) {
            log.warn("人工解绑能力已关闭（unbind.enabled=false）");
            throw business(UserVehicleBindingErrorCode.INVALID_REQUEST);
        }
    }

    private void audit(String bizType, String bizId, String vin, String accountId, String fromState, String toState,
                       String operatorSource, String reason, Instant now) {
        userVehicleAuditRepository.insert(UserVehicleAuditRecord.builder()
                .auditId(newId("AUD"))
                .bizType(bizType)
                .bizId(bizId)
                .vin(vin)
                .accountId(accountId)
                .fromState(fromState)
                .toState(toState)
                .operatorSource(operatorSource)
                .reason(reason)
                .occurredAt(now)
                .build());
    }

    private BusinessException business(UserVehicleBindingErrorCode errorCode) {
        return new BusinessException(errorCode);
    }

    private String newId(String prefix) {
        return prefix + IdUtil.fastSimpleUUID().toUpperCase();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean equalsText(String left, String right) {
        return left != null && left.equals(right);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private String maskVin(String vin) {
        if (vin == null || vin.length() <= 7) {
            return "***";
        }
        return vin.substring(0, 3) + "***" + vin.substring(vin.length() - 4);
    }
}
