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
    NOT_GRANTED
}
