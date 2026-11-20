package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingCallerScope;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingEligibility;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingResultReason;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.BindingOperation;

import java.time.Instant;
import java.util.Optional;

/**
 * 绑定确认操作仓储（幂等台账）。
 *
 * @author hwyz_leo
 */
public interface BindingOperationRepository {

    /**
     * 新增操作
     *
     * @param operation 操作
     */
    void insert(BindingOperation operation);

    /**
     * 按操作ID查询
     *
     * @param operationId 操作ID
     * @return 操作
     */
    Optional<BindingOperation> findByOperationId(String operationId);

    /**
     * 按幂等键查询
     *
     * @param callerScope    调用方范围
     * @param accountId      账号
     * @param idempotencyKey 幂等键
     * @return 操作
     */
    Optional<BindingOperation> findByIdempotencyKey(BindingCallerScope callerScope, String accountId,
                                                   String idempotencyKey);

    /**
     * 锁定读取操作（事务内）
     *
     * @param operationId 操作ID
     * @return 操作
     */
    Optional<BindingOperation> lockByOperationId(String operationId);

    /**
     * 锁定读取幂等键（事务内）
     *
     * @param callerScope    调用方范围
     * @param accountId      账号
     * @param idempotencyKey 幂等键
     * @return 操作
     */
    Optional<BindingOperation> lockByIdempotencyKey(BindingCallerScope callerScope, String accountId,
                                                    String idempotencyKey);

    /**
     * 标记成功
     *
     * @param operationId     操作ID
     * @param relationId      关系ID
     * @param factRef         事实引用
     * @param factCheckedAt   事实读取时刻
     * @param now             终态时刻
     * @param expectedVersion 期望版本
     * @return 更新成功返回 true
     */
    boolean markSucceeded(String operationId, String relationId, String factRef, Instant factCheckedAt,
                          Instant now, int expectedVersion);

    /**
     * 标记失败（业务拒绝/冲突；瞬时故障不得写入终态）
     *
     * @param operationId     操作ID
     * @param reason          语义 reason
     * @param eligibility     资格判定
     * @param ruleVersion     规则版本
     * @param factRef         事实引用
     * @param factCheckedAt   事实读取时刻
     * @param now             终态时刻
     * @param expectedVersion 期望版本
     * @return 更新成功返回 true
     */
    boolean markFailed(String operationId, BindingResultReason reason, BindingEligibility eligibility,
                       String ruleVersion, String factRef, Instant factCheckedAt, Instant now, int expectedVersion);
}
