package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

import java.util.Arrays;

/**
 * 车辆接入身份解析状态枚举类
 *
 * @author hwyz_leo
 */
public enum ResolveStatus {

    /** 解析成功 **/
    RESOLVED,
    /** 未解析（fail-closed） **/
    UNRESOLVED;

    public static ResolveStatus valOf(String val) {
        return Arrays.stream(ResolveStatus.values())
                .filter(status -> status.name().equals(val))
                .findFirst()
                .orElse(null);
    }
}
