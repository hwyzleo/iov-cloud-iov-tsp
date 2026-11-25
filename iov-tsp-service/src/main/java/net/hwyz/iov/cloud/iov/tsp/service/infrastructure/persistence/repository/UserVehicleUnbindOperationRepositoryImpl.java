package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CredentialRevocationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UnbindRelationState;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.UtcTime;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleUnbindOperation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleUnbindOperationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.UserVehicleUnbindOperationMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.UserVehicleUnbindOperationPo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 人工解绑与凭证撤销台账仓储实现（IOV-TSP-DSN-CR-021）。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class UserVehicleUnbindOperationRepositoryImpl implements UserVehicleUnbindOperationRepository {

    private final UserVehicleUnbindOperationMapper userVehicleUnbindOperationMapper;

    @Override
    public void insert(UserVehicleUnbindOperation operation) {
        UserVehicleUnbindOperationPo po = UserVehicleUnbindOperationPo.builder()
                .operationId(operation.getOperationId())
                .relationId(operation.getRelationId())
                .accountId(operation.getAccountId())
                .vin(operation.getVin())
                .operatorId(operation.getOperatorId())
                .idempotencyKey(operation.getIdempotencyKey())
                .requestDigest(operation.getRequestDigest())
                .expectedRowVersion(operation.getExpectedRowVersion())
                .reason(operation.getReason())
                .relationState(operation.getRelationState().name())
                .credentialRevocationStatus(operation.getCredentialRevocationStatus().name())
                .build();
        userVehicleUnbindOperationMapper.insert(po);
        operation.setId(po.getId());
    }

    @Override
    public Optional<UserVehicleUnbindOperation> findByOperationId(String operationId) {
        return Optional.ofNullable(userVehicleUnbindOperationMapper.selectByOperationId(operationId))
                .map(this::toEntity);
    }

    @Override
    public Optional<UserVehicleUnbindOperation> findByOperatorAndKey(String operatorId, String idempotencyKey) {
        return Optional.ofNullable(userVehicleUnbindOperationMapper.selectByOperatorAndKey(operatorId, idempotencyKey))
                .map(this::toEntity);
    }

    @Override
    public boolean markReleased(String operationId, Instant releasedAt, Instant completedAt, Instant nextRetryAt,
                                Instant now, int expectedVersion) {
        return userVehicleUnbindOperationMapper.markReleased(operationId, UtcTime.toLocalDateTime(releasedAt),
                UtcTime.toLocalDateTime(completedAt), UtcTime.toLocalDateTime(nextRetryAt),
                UtcTime.toLocalDateTime(now), expectedVersion) > 0;
    }

    @Override
    public boolean markRevocationSucceeded(String operationId, int revokedSessionCount, Instant now,
                                           int expectedVersion) {
        return userVehicleUnbindOperationMapper.markRevocationSucceeded(operationId, revokedSessionCount,
                UtcTime.toLocalDateTime(now), expectedVersion) > 0;
    }

    @Override
    public boolean markRevocationFailed(String operationId, String lastError, Instant nextRetryAt,
                                        boolean incrementRetry, Instant now, int expectedVersion) {
        return userVehicleUnbindOperationMapper.markRevocationFailed(operationId, lastError,
                UtcTime.toLocalDateTime(nextRetryAt), incrementRetry ? 1 : 0, UtcTime.toLocalDateTime(now),
                expectedVersion) > 0;
    }

    @Override
    public List<UserVehicleUnbindOperation> findRevocationRetryCandidates(Instant now, int maxRetryCount, int limit) {
        return userVehicleUnbindOperationMapper.selectRevocationRetryCandidates(UtcTime.toLocalDateTime(now),
                        maxRetryCount, limit).stream()
                .map(this::toEntity)
                .toList();
    }

    private UserVehicleUnbindOperation toEntity(UserVehicleUnbindOperationPo po) {
        return UserVehicleUnbindOperation.builder()
                .id(po.getId())
                .operationId(po.getOperationId())
                .relationId(po.getRelationId())
                .accountId(po.getAccountId())
                .vin(po.getVin())
                .operatorId(po.getOperatorId())
                .idempotencyKey(po.getIdempotencyKey())
                .requestDigest(po.getRequestDigest())
                .expectedRowVersion(po.getExpectedRowVersion())
                .reason(po.getReason())
                .relationState(po.getRelationState() == null ? null : UnbindRelationState.valueOf(po.getRelationState()))
                .releasedAt(UtcTime.toInstant(po.getReleasedAt()))
                .completedAt(UtcTime.toInstant(po.getCompletedAt()))
                .credentialRevocationStatus(po.getCredentialRevocationStatus() == null ? null
                        : CredentialRevocationStatus.valueOf(po.getCredentialRevocationStatus()))
                .revokedSessionCount(po.getRevokedSessionCount())
                .retryCount(po.getRetryCount())
                .nextRetryAt(UtcTime.toInstant(po.getNextRetryAt()))
                .lastError(po.getLastError())
                .rowVersion(po.getRowVersion())
                .build();
    }
}
