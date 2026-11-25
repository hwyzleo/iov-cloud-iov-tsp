package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

/**
 * 整车首次激活事实查询状态（IOV-TSP-DSN-CR-021 D021-02）。
 * <p>
 * RECORDED/PENDING/NOT_RECORDED 来自 VMD 官方只读契约；
 * UNKNOWN 仅表示查询失败或响应非法，不得与 NOT_RECORDED 混用。
 *
 * @author hwyz_leo
 */
public enum FirstActivationState {

    /**
     * VMD 已记录首次激活事实
     */
    RECORDED,

    /**
     * VMD 已接收事实但尚未作用于投影
     */
    PENDING,

    /**
     * VMD 无任何首次激活事实
     */
    NOT_RECORDED,

    /**
     * 查询失败、超时或响应非法（数据区不可用）
     */
    UNKNOWN
}
