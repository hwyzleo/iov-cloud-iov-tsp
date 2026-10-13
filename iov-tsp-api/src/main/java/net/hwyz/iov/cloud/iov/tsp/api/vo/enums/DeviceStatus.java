package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

import java.util.Arrays;

/**
 * 设备生命周期准入态枚举（TSP-DSN-CR-011 §4.4）
 * <p>
 * 口径沿用 TSP 设备状态机：PRE_ACTIVE / ACTIVATING / ACTIVE 可继续准入判定，
 * BLOCKED / RETIRED 拒绝；首次设备必须能在准入后进入 ACTIVATING。
 *
 * @author hwyz_leo
 */
public enum DeviceStatus {

    /** 待激活（尚未成功接入） **/
    PRE_ACTIVE,
    /** 已接入，正在完成 V2C 初始密钥开通 **/
    ACTIVATING,
    /** 上下行业务密钥均已导入确认 **/
    ACTIVE,
    /** 冻结/黑名单 **/
    BLOCKED,
    /** 报废 **/
    RETIRED;

    public static DeviceStatus valOf(String val) {
        return Arrays.stream(values())
                .filter(status -> status.name().equals(val))
                .findFirst()
                .orElse(null);
    }
}
