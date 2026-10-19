package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model;

/**
 * 业务激活处理结果
 * <p>
 * TSP-DSN-CR-010 R4：领域服务只更新 {@code tb_tbox}，事件表写入由应用服务负责。
 * TSP-DSN-CR-012 §6/§8：按“成功 / 有界重试 / 永久拒绝”三分语义返回，
 * 供应用层区分是否落库 Inbox 成功事实与是否进入 DLT。
 *
 * @author hwyz_leo
 */
public enum ActivationOutcome {

    /**
     * ACTIVATING -> ACTIVE 条件更新成功
     */
    ACTIVATED,

    /**
     * 设备已为 ACTIVE，幂等成功（不覆盖 activateTime）
     */
    IDEMPOTENT_ACTIVE,

    /**
     * 设备仍为 PRE_ACTIVE（跨 Topic 乱序），需有界重试等待 DEVICE_ONLINE
     */
    RETRY_OUT_OF_ORDER,

    /**
     * 设备不存在，可能有界重试后进入人工对账
     */
    RETRY_DEVICE_NOT_FOUND,

    /**
     * 无唯一 active 绑定投影（投影延迟），可能有界重试后进入人工对账
     */
    RETRY_BINDING_MISSING,

    /**
     * 条件更新未命中且状态仍不允许激活（并发/瞬时），可重试
     */
    RETRY_TRANSIENT,

    /**
     * 契约校验不通过，永久拒绝，不激活
     */
    REJECTED_CONTRACT,

    /**
     * BLOCKED/RETIRED/非法状态，永久拒绝 + 告警
     */
    REJECTED_DEVICE_STATE,

    /**
     * 多条 active 绑定（数据完整性错误），永久拒绝，不重试
     */
    REJECTED_BINDING_CONFLICT
}
