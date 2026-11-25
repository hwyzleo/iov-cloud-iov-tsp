package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

/**
 * MPT 车辆详情用户绑定状态（IOV-TSP-DSN-CR-021 D021-02）。
 * <p>
 * BOUND 仅表示当前存在 ACTIVE 用户—VIN 主使用人关系；不表示车机已登录、
 * 首次激活已记录或凭证撤销已完成。
 *
 * @author hwyz_leo
 */
public enum UserVehicleBindingStatus {

    /**
     * 当前存在 ACTIVE 关系
     */
    BOUND,

    /**
     * 无 ACTIVE 关系（可返回最近一次已释放关系作为历史信息）
     */
    UNBOUND
}
