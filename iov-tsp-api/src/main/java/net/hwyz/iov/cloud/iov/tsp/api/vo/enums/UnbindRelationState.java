package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

/**
 * 人工解绑操作的关系状态（IOV-TSP-DSN-CR-021 D021-03/D021-05）。
 * <p>
 * 本地事务提交后固定为 RELEASED；PENDING 仅表示事务尚未完成，不作为对外稳定结果。
 *
 * @author hwyz_leo
 */
public enum UnbindRelationState {

    /**
     * 解绑操作已受理，关系尚未释放（事务中）
     */
    PENDING,

    /**
     * 关系已在本地权威释放
     */
    RELEASED
}
