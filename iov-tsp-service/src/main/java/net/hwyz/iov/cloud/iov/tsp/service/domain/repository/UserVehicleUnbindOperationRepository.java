package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleUnbindOperation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 用户车辆关系人工解绑操作与 CIAM 凭证撤销台账仓储（IOV-TSP-DSN-CR-021 D021-04/D021-05）。
 * <p>
 * 唯一键 {@code uk_uvuo_operator_idempotency} 是并发幂等的最终防线；
 * 撤销结果以条件更新推进，重试以持久化 {@code next_retry_at} 驱动。
 *
 * @author hwyz_leo
 */
public interface UserVehicleUnbindOperationRepository {

    /**
     * 新增解绑操作（唯一键冲突由应用层重读判定）
     *
     * @param operation 操作
     */
    void insert(UserVehicleUnbindOperation operation);

    /**
     * 按操作ID查询
     *
     * @param operationId 操作ID
     * @return 操作
     */
    Optional<UserVehicleUnbindOperation> findByOperationId(String operationId);

    /**
     * 按操作人与幂等键查询
     *
     * @param operatorId     操作人
     * @param idempotencyKey 幂等键
     * @return 操作
     */
    Optional<UserVehicleUnbindOperation> findByOperatorAndKey(String operatorId, String idempotencyKey);

    /**
     * 标记关系已释放（PENDING → RELEASED，含撤销初始 PENDING 与下次重试时刻）
     *
     * @param operationId     操作ID
     * @param releasedAt      关系释放绝对时刻
     * @param completedAt     本地事务完成绝对时刻
     * @param nextRetryAt     下次撤销重试绝对时刻（可为空）
     * @param now             修改绝对时刻
     * @param expectedVersion 预期版本
     * @return 更新成功返回 true
     */
    boolean markReleased(String operationId, Instant releasedAt, Instant completedAt, Instant nextRetryAt,
                         Instant now, int expectedVersion);

    /**
     * 标记撤销成功（条件：关系已 RELEASED 且撤销非 SUCCEEDED）
     *
     * @param operationId         操作ID
     * @param revokedSessionCount 本次撤销会话数
     * @param now                 修改绝对时刻
     * @param expectedVersion     预期版本
     * @return 更新成功返回 true（0 行表示已被并发推进）
     */
    boolean markRevocationSucceeded(String operationId, int revokedSessionCount, Instant now, int expectedVersion);

    /**
     * 标记撤销失败（条件：关系已 RELEASED 且撤销非 SUCCEEDED）
     *
     * @param operationId     操作ID
     * @param lastError       失败摘要（脱敏、截断）
     * @param nextRetryAt     下次重试绝对时刻（为空表示不再自动重试）
     * @param incrementRetry  是否累加重试次数
     * @param now             修改绝对时刻
     * @param expectedVersion 预期版本
     * @return 更新成功返回 true
     */
    boolean markRevocationFailed(String operationId, String lastError, Instant nextRetryAt, boolean incrementRetry,
                                 Instant now, int expectedVersion);

    /**
     * 查询待自动重试的撤销操作（RELEASED + PENDING/FAILED + next_retry_at <= now + 重试次数未耗尽）
     *
     * @param now            当前绝对时刻
     * @param maxRetryCount  自动重试上限
     * @param limit          单批数量
     * @return 待重试操作
     */
    List<UserVehicleUnbindOperation> findRevocationRetryCandidates(Instant now, int maxRetryCount, int limit);
}
