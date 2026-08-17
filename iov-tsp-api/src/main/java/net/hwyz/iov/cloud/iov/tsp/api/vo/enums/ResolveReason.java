package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

import java.util.Arrays;

/**
 * 车辆接入身份解析原因枚举类
 *
 * @author hwyz_leo
 */
public enum ResolveReason {

    /** 解析成功 **/
    RESOLVED,
    /** VIN 不存在或非法 **/
    VIN_UNKNOWN,
    /** 车辆无当前有效（ACTIVE）TBOX 绑定 **/
    UNBOUND,
    /** 同一 VIN 存在多条 ACTIVE TBOX 绑定，fail-closed **/
    BINDING_CONFLICT,
    /** 绑定投影关联的 TBOX 设备不存在 **/
    TBOX_NOT_FOUND,
    /** TBOX 设备缺少 HSM UID **/
    HSM_UID_MISSING,
    /** 依赖不可用（数据库/外部依赖异常），禁止返回伪身份 **/
    DEPENDENCY_UNAVAILABLE;

    public static ResolveReason valOf(String val) {
        return Arrays.stream(ResolveReason.values())
                .filter(reason -> reason.name().equals(val))
                .findFirst()
                .orElse(null);
    }
}
