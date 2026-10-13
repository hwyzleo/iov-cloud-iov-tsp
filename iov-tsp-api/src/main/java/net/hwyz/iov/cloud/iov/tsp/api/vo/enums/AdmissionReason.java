package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

import java.util.Arrays;

/**
 * 设备接入裁决原因枚举（TSP-DSN-CR-011 §4.4）
 * <p>
 * 语义枚举，禁止由自由文本代替；VAGW 据此映射 EMQX 错误码 804001/804002。
 *
 * @author hwyz_leo
 */
public enum AdmissionReason {

    /** 无（仅 ALLOW 使用） **/
    NONE,
    /** uid 在 TSP 设备事实中不存在 **/
    UID_UNKNOWN,
    /** 设备无当前有效（ACTIVE）车辆绑定 **/
    UNBOUND,
    /** 同一设备存在多条 ACTIVE 绑定，fail-closed **/
    BINDING_CONFLICT,
    /** 证书已被吊销 **/
    CERT_REVOKED,
    /** 证书已过期（TSP 冗余兜底） **/
    CERT_EXPIRED,
    /** 设备被冻结/黑名单 **/
    DEVICE_BLOCKED,
    /** 设备已报废 **/
    DEVICE_RETIRED,
    /** PKI 吊销依赖不可用或结果不确定，fail-closed **/
    DEPENDENCY_UNAVAILABLE;

    public static AdmissionReason valOf(String val) {
        return Arrays.stream(values())
                .filter(reason -> reason.name().equals(val))
                .findFirst()
                .orElse(null);
    }
}
