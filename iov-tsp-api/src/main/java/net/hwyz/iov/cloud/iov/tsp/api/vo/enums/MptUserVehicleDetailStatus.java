package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

/**
 * MPT 车辆详情数据完整度（IOV-TSP-DSN-CR-021 D021-02）。
 * <p>
 * 外部只读依赖（VMD 首次激活）不可用或响应非法时返回 PARTIAL，
 * 核心本地关系事实仍可用，不伪造外部状态。
 *
 * @author hwyz_leo
 */
public enum MptUserVehicleDetailStatus {

    /**
     * 全部数据区可用
     */
    COMPLETE,

    /**
     * 部分数据区不可用（外部只读依赖故障或响应非法）
     */
    PARTIAL
}
