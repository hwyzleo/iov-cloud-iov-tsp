package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * TBOX 生命周期时间线里程碑（只读、可证实）
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum TboxTimelineMilestone {

    IMPORTED("设备导入"),
    FIRST_LOGIN("首次登录"),
    ACTIVATING("激活中"),
    V2C_PROVISIONED("V2C密钥开通"),
    ACTIVE("已激活"),
    LAST_LOGIN("最近登录"),
    BLOCKED("已冻结"),
    RETIRED("已退役");

    private final String desc;
}
