package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingCallerScope;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingEligibility;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingOperationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingResultReason;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.UtcTime;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.BindingOperation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.BindingOperationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.BindingOperationMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.BindingOperationPo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * 绑定确认操作仓储实现。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class BindingOperationRepositoryImpl implements BindingOperationRepository {

    private final BindingOperationMapper bindingOperationMapper;

    @Override
    public void insert(BindingOperation operation) {
        BindingOperationPo po = BindingOperationPo.builder()
                .operationId(operation.getOperationId())
                .callerScope(operation.getCallerScope() == null ? null : operation.getCallerScope().name())
                .accountId(operation.getAccountId())
                .idempotencyKey(operation.getIdempotencyKey())
                .requestDigest(operation.getRequestDigest())
                .sessionId(operation.getSessionId())
                .vin(operation.getVin())
                .status(operation.getStatus() == null ? BindingOperationStatus.PENDING.name() : operation.getStatus().name())
                .eligibility(operation.getEligibility() == null ? null : operation.getEligibility().name())
                .ruleVersion(operation.getRuleVersion())
                .factRef(operation.getFactRef())
                .factCheckedAt(UtcTime.toLocalDateTime(operation.getFactCheckedAt()))
                .build();
        bindingOperationMapper.insert(po);
        operation.setId(po.getId());
    }

    @Override
    public Optional<BindingOperation> findByOperationId(String operationId) {
        return Optional.ofNullable(bindingOperationMapper.selectByOperationId(operationId)).map(this::toEntity);
    }

    @Override
    public Optional<BindingOperation> findByIdempotencyKey(BindingCallerScope callerScope, String accountId,
                                                          String idempotencyKey) {
        return Optional.ofNullable(bindingOperationMapper.selectByIdempotencyKey(
                callerScope.name(), accountId, idempotencyKey)).map(this::toEntity);
    }

    @Override
    public Optional<BindingOperation> lockByOperationId(String operationId) {
        return Optional.ofNullable(bindingOperationMapper.selectByOperationIdForUpdate(operationId)).map(this::toEntity);
    }

    @Override
    public Optional<BindingOperation> lockByIdempotencyKey(BindingCallerScope callerScope, String accountId,
                                                           String idempotencyKey) {
        return Optional.ofNullable(bindingOperationMapper.selectByIdempotencyKeyForUpdate(
                callerScope.name(), accountId, idempotencyKey)).map(this::toEntity);
    }

    @Override
    public boolean markSucceeded(String operationId, String relationId, String factRef, Instant factCheckedAt,
                                 Instant now, int expectedVersion) {
        return bindingOperationMapper.markSucceeded(operationId, relationId, factRef,
                UtcTime.toLocalDateTime(factCheckedAt), UtcTime.toLocalDateTime(now), expectedVersion) > 0;
    }

    @Override
    public boolean markFailed(String operationId, BindingResultReason reason, BindingEligibility eligibility,
                              String ruleVersion, String factRef, Instant factCheckedAt, Instant now,
                              int expectedVersion) {
        return bindingOperationMapper.markFailed(operationId, reason == null ? null : reason.name(),
                eligibility == null ? null : eligibility.name(), ruleVersion, factRef,
                UtcTime.toLocalDateTime(factCheckedAt), UtcTime.toLocalDateTime(now), expectedVersion) > 0;
    }

    private BindingOperation toEntity(BindingOperationPo po) {
        return BindingOperation.builder()
                .id(po.getId())
                .operationId(po.getOperationId())
                .callerScope(po.getCallerScope() == null ? null : BindingCallerScope.valueOf(po.getCallerScope()))
                .accountId(po.getAccountId())
                .idempotencyKey(po.getIdempotencyKey())
                .requestDigest(po.getRequestDigest())
                .sessionId(po.getSessionId())
                .vin(po.getVin())
                .status(po.getStatus() == null ? null : BindingOperationStatus.valueOf(po.getStatus()))
                .resultReason(po.getResultReason() == null ? null : BindingResultReason.valueOf(po.getResultReason()))
                .resultRelationId(po.getResultRelationId())
                .eligibility(po.getEligibility() == null ? null : BindingEligibility.valueOf(po.getEligibility()))
                .ruleVersion(po.getRuleVersion())
                .factRef(po.getFactRef())
                .factCheckedAt(UtcTime.toInstant(po.getFactCheckedAt()))
                .completedAt(UtcTime.toInstant(po.getCompletedAt()))
                .rowVersion(po.getRowVersion())
                .build();
    }
}
