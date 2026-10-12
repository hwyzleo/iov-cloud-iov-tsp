package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * TBOX 运营异常标签（查询时派生，不改变 DeviceStatus）
 * <p>
 * priority 仅影响展示排序，不改变原始设备状态。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum TboxAnomalyType {

    BINDING_CONFLICT(1, "绑定冲突"),
    FROZEN_OR_RETIRED(2, "冻结或退役"),
    ACTIVATION_TIMEOUT(3, "激活超时"),
    NEVER_ONLINE(4, "从未上线"),
    LONG_OFFLINE(5, "长期未登录"),
    UNBOUND(6, "未绑定");

    private final int priority;
    private final String desc;
}
