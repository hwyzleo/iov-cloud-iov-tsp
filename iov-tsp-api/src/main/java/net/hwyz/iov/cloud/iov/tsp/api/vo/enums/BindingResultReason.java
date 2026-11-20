package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

/**
 * 绑定/会话业务语义 reason（IOV-TSP-DSN-CR-017 D017-04）。
 * <p>
 * 数字错误码在 service 侧按 401xxx 段登记；本枚举用于结果与审计中的稳定语义标识。
 *
 * @author hwyz_leo
 */
public enum BindingResultReason {

    /**
     * 会话不存在或无效（不区分不可枚举场景）
     */
    SESSION_INVALID,

    /**
     * 会话已过期
     */
    SESSION_EXPIRED,

    /**
     * 会话已取消/已跳过
     */
    SESSION_CANCELLED,

    /**
     * 会话已被其他账号认领
     */
    SESSION_CLAIMED,

    /**
     * 车辆事实不存在
     */
    VEHICLE_NOT_FOUND,

    /**
     * 资格明确不符
     */
    ELIGIBILITY_REJECTED,

    /**
     * 资格无法确定
     */
    ELIGIBILITY_UNDETERMINED,

    /**
     * 依赖不可用（VSO/CMS/VMD）
     */
    DEPENDENCY_UNAVAILABLE,

    /**
     * 该 VIN 已存在其他账号的主使用人关系
     */
    RELATION_CONFLICT,

    /**
     * 同幂等键不同请求内容
     */
    IDEMPOTENCY_CONFLICT,

    /**
     * 处理中
     */
    PENDING,

    /**
     * 协议未明确同意或修订不匹配
     */
    AGREEMENT_NOT_ACCEPTED,

    /**
     * 回执/同意补报内容冲突（同ID不同内容）
     */
    REPORT_CONFLICT,

    /**
     * 请求参数非法（含可信上下文缺失）
     */
    INVALID_REQUEST
}
