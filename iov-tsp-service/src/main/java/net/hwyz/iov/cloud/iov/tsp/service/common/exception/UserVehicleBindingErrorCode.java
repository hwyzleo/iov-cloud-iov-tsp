package net.hwyz.iov.cloud.iov.tsp.service.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hwyz.iov.cloud.framework.common.exception.ErrorCode;

/**
 * 用户车辆关系业务错误码（IOV-TSP-DSN-CR-017）。
 * <p>
 * 分配在 IOV-TSP 的 401xxx 段：使用 4011xx 子段，避开已退役的设备准入 401001～401016
 * （退役号不复用，历史映射见 SEC-VIAM 的 legacy 码映射）。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum UserVehicleBindingErrorCode implements ErrorCode {

    /**
     * 会话不存在或无效（不区分「查不到」以防枚举）
     */
    SESSION_INVALID("401101", "激活会话无效"),

    /**
     * 会话已过期（服务端 Clock 判定）
     */
    SESSION_EXPIRED("401102", "激活会话已过期"),

    /**
     * 会话已取消/已跳过
     */
    SESSION_CANCELLED("401103", "激活会话已取消"),

    /**
     * 会话已被其他账号认领
     */
    SESSION_CLAIMED("401104", "激活会话已被其他账号占用"),

    /**
     * 车辆事实不存在
     */
    VEHICLE_NOT_FOUND("401105", "车辆不存在或不可用于绑车"),

    /**
     * 资格明确不符（未交付/无账号-车辆关联/角色不符）
     */
    ELIGIBILITY_REJECTED("401106", "绑车资格不符，请按交付人员或客服指引核验"),

    /**
     * 资格无法确定（规则缺失/事实不足/歧义）
     */
    ELIGIBILITY_UNDETERMINED("401107", "绑车资格无法确定，请按交付人员或客服指引核验"),

    /**
     * 依赖不可用（VSO/CMS/VMD）
     */
    DEPENDENCY_UNAVAILABLE("401108", "依赖服务暂不可用，请稍后重试"),

    /**
     * 该 VIN 已存在其他账号的主使用人关系
     */
    RELATION_CONFLICT("401109", "该车辆已存在其他用户的主使用人关系"),

    /**
     * 同幂等键不同请求内容
     */
    IDEMPOTENCY_CONFLICT("401110", "幂等键对应的请求内容不一致"),

    /**
     * 协议未明确同意或修订不匹配
     */
    AGREEMENT_NOT_ACCEPTED("401111", "协议未明确同意或修订不一致"),

    /**
     * 请求参数非法（含可信上下文缺失）
     */
    INVALID_REQUEST("401112", "请求参数非法"),

    /**
     * 回执冲突（同ID不同内容）
     */
    REPORT_CONFLICT("401113", "回执内容冲突"),

    /**
     * 授权回执版本早于已存版本
     */
    AUTHORIZATION_VERSION_CONFLICT("401114", "授权回执版本早于已记录版本");

    private final String code;
    private final String message;
}
