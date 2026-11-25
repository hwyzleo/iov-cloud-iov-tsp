package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

/**
 * 隐私授权状态（IOV-TSP-DSN-CR-017 D017-06）。
 * <p>
 * 关闭授权不等于 GRANTED；未记录不等于同意。
 *
 * @author hwyz_leo
 */
public enum PrivacyAuthorizationState {

    /**
     * 已授权
     */
    GRANTED,

    /**
     * 明确拒绝
     */
    DENIED,

    /**
     * 未授权（含用户关闭弹框）
     */
    NOT_GRANTED,

    /**
     * 历史关系周期失效（IOV-TSP-DSN-CR-021 D021-05/D021-08）。
     * <p>
     * 仅用于迁移无法归属当前 ACTIVE 关系的存量记录；解绑后旧关系授权不再有效，
     * 但历史状态保留、不物理删除。该值不接受车端上报。
     */
    INVALIDATED
}
