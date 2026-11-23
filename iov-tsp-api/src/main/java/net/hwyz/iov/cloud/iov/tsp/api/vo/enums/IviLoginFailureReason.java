package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

/**
 * 车机账号登录失败原因（IOV-TSP-DSN-CR-020 D020-06，§13 裁决 D 冻结）。
 * <p>
 * 取值集与车端 IVI-ACC {@code encodeLoginResult} 一致，两侧不得各自扩展；
 * 字段可选，FAILED 时建议携带，非法取值按 401112 拒绝。
 *
 * @author hwyz_leo
 */
public enum IviLoginFailureReason {

    /**
     * 用户主动取消登录
     */
    USER_CANCELLED,

    /**
     * 账号不匹配（车机登录账号与绑车账号不一致）
     */
    ACCOUNT_MISMATCH,

    /**
     * 票据兑换失败
     */
    TOKEN_EXCHANGE_FAILED,

    /**
     * 车机侧登录超时
     */
    TIMEOUT,

    /**
     * 车机侧网络错误
     */
    NETWORK_ERROR,

    /**
     * 其它未知原因
     */
    UNKNOWN
}
