package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

import java.util.Arrays;

/**
 * 车辆-设备绑定状态枚举（TSP-DSN-CR-011 §4.4）
 *
 * @author hwyz_leo
 */
public enum BindStatus {

    /** 存在唯一 ACTIVE 绑定 **/
    BOUND,
    /** 无 ACTIVE 绑定 **/
    UNBOUND,
    /** 存在多条 ACTIVE 绑定（冲突，fail-closed） **/
    CONFLICT,
    /** 尚未判定或不可确定 **/
    UNKNOWN;

    public static BindStatus valOf(String val) {
        return Arrays.stream(values())
                .filter(status -> status.name().equals(val))
                .findFirst()
                .orElse(null);
    }
}
