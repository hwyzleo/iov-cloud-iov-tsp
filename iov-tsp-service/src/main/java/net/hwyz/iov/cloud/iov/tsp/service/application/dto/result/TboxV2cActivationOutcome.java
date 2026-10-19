package net.hwyz.iov.cloud.iov.tsp.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * VMD V2C 聚合成功事件消费结果（TSP-DSN-CR-012 §5/§8）
 * <p>
 * 区分成功/幂等、有界重试（跨 Topic 乱序、投影延迟、瞬时基础设施）与永久拒绝（DLT + 告警）。
 * 消费者据此决定是否 ack，不得把契约/状态拒绝做自动重试。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum TboxV2cActivationOutcome {

    /**
     * ACTIVATING -> ACTIVE 已完成，Inbox 成功事实已同事务落库
     */
    ACTIVATED(true, false),

    /**
     * 设备已为 ACTIVE（幂等），Inbox 成功事实已落库
     */
    IDEMPOTENT_ACTIVE(true, false),

    /**
     * 事件（eventId 或 requestId）已消费且摘要一致，幂等成功
     */
    DUPLICATE(true, false),

    /**
     * PRE_ACTIVE 早于 DEVICE_ONLINE，有界重试等待上线事件
     */
    RETRY_OUT_OF_ORDER(false, true),

    /**
     * 设备不存在，有界重试耗尽后 DLT 人工对账
     */
    RETRY_DEVICE_NOT_FOUND(false, true),

    /**
     * active 绑定投影缺失，有界重试耗尽后 DLT 人工对账
     */
    RETRY_BINDING_MISSING(false, true),

    /**
     * 并发/瞬时条件更新未命中，有界重试
     */
    RETRY_TRANSIENT(false, true),

    /**
     * 契约不合法（防御性），永久拒绝
     */
    PERMANENT_CONTRACT_INVALID(false, false),

    /**
     * 同 eventId / 同 requestId 但契约摘要冲突，永久拒绝并告警
     */
    PERMANENT_CONTRACT_CONFLICT(false, false),

    /**
     * BLOCKED/RETIRED/非法设备状态，永久拒绝并告警
     */
    PERMANENT_DEVICE_STATE(false, false),

    /**
     * 多条 active 绑定，永久拒绝并告警
     */
    PERMANENT_BINDING_CONFLICT(false, false);

    /**
     * 是否为成功终态（可提交 offset）
     */
    private final boolean success;

    /**
     * 是否为可重试结果（有界退避后仍失败则进入 DLT）
     */
    private final boolean retryable;
}
