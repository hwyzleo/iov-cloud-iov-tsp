package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

/**
 * 车机账号登录结果（与云端绑定关系独立）。
 *
 * @author hwyz_leo
 */
public enum IviLoginResultStatus {

    /**
     * 登录成功
     */
    SUCCEEDED,

    /**
     * 登录失败（只影响车机登录，不改变关系）
     */
    FAILED
}
