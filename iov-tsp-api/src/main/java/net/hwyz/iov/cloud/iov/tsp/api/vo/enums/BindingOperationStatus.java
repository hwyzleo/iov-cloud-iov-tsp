package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

/**
 * 绑定确认操作状态（IOV-TSP-DSN-CR-017 D017-04）。
 * <p>
 * PENDING 表示处理中（不可伪装为失败或成功）；终态 SUCCEEDED/FAILED 通过幂等键返回。
 *
 * @author hwyz_leo
 */
public enum BindingOperationStatus {

    /**
     * 处理中
     */
    PENDING,

    /**
     * 已成功（关系已提交）
     */
    SUCCEEDED,

    /**
     * 已失败（业务拒绝或冲突，含语义 reason）
     */
    FAILED
}
