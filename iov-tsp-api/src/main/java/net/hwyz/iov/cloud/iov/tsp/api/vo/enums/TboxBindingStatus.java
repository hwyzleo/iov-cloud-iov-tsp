package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * TBOX 当前绑定状态（查询时按 active 绑定投影数量派生）
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum TboxBindingStatus {

    /**
     * 0 条 active 绑定
     */
    UNBOUND("未绑定"),

    /**
     * 1 条 active 绑定
     */
    BOUND("已绑定"),

    /**
     * 多条 active 绑定
     */
    BINDING_CONFLICT("绑定冲突");

    private final String desc;
}
