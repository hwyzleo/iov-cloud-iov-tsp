package net.hwyz.iov.cloud.iov.tsp.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DEVICE_ONLINE 处理结果
 * <p>
 * TSP-DSN-CR-010 §9：区分成功/幂等、可重试（对账窗口）与永久拒绝（进入 DLT）。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum DeviceOnlineOutcome {

    /**
     * 已成功记录登录并推进状态
     */
    PROCESSED(true),

    /**
     * 重复事件，幂等成功
     */
    DUPLICATE(true),

    /**
     * 设备不存在，可对账重试
     */
    RETRY_TBOX_NOT_FOUND(false),

    /**
     * 无唯一 active 绑定（UNBOUND），可对账重试
     */
    RETRY_UNBOUND(false),

    /**
     * HSM 身份不唯一（多行），永久拒绝
     */
    PERMANENT_TBOX_IDENTITY_CONFLICT(false),

    /**
     * tboxSn 与本地设备不一致，永久拒绝
     */
    PERMANENT_TBOX_SN_MISMATCH(false),

    /**
     * event.vin 与当前 active VIN 不一致，永久拒绝
     */
    PERMANENT_VIN_MISMATCH(false),

    /**
     * 多条 active 绑定（BINDING_CONFLICT），永久拒绝
     */
    PERMANENT_BINDING_CONFLICT(false),

    /**
     * 同 eventId 但内容摘要冲突（CONTRACT_CONFLICT），永久拒绝
     */
    PERMANENT_CONTRACT_CONFLICT(false);

    /**
     * 是否为可提交 offset 的终态（成功/幂等）
     */
    private final boolean success;
}
