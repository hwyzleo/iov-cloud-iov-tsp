package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

import java.util.Arrays;

/**
 * 设备接入裁决结果枚举（TSP-DSN-CR-011 §4.4）
 *
 * @author hwyz_leo
 */
public enum AdmissionDecision {

    /** 允许接入 **/
    ALLOW,
    /** 拒绝接入 **/
    DENY;

    public static AdmissionDecision valOf(String val) {
        return Arrays.stream(values())
                .filter(decision -> decision.name().equals(val))
                .findFirst()
                .orElse(null);
    }
}
