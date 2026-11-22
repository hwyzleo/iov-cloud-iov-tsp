package net.hwyz.iov.cloud.iov.tsp.service.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hwyz.iov.cloud.framework.common.exception.ErrorCode;

/**
 * 一次性登录票据业务错误码（IOV-TSP-DSN-CR-019 D019-06）。
 * <p>
 * 401115～401117 为本 CR 新分配的票据错误码，语义与既有 401101～401114 不重叠；
 * 限流沿用平台可重试的错误码 701003。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum ActivationLoginTicketErrorCode implements ErrorCode {

    /**
     * 票据不可用（不存在/状态非 ISSUED/车辆不匹配/登录已完成/签发条件不满足）
     */
    LOGIN_TICKET_UNAVAILABLE("401115", "登录票据不可用"),

    /**
     * 票据已消费（单次消费，不返回绑定事实）
     */
    LOGIN_TICKET_CONSUMED("401116", "登录票据已消费"),

    /**
     * 票据已过期（服务端 Clock 判定）
     */
    LOGIN_TICKET_EXPIRED("401117", "登录票据已过期"),

    /**
     * 签发限流（会话维度/总次数超限，可稍后重试）
     */
    RATE_LIMITED("701003", "请求过于频繁，请稍后重试");

    private final String code;
    private final String message;
}
