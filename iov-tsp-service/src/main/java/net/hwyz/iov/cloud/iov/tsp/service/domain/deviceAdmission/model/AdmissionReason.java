package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model;

/**
 * 设备接入裁决原因（TSP-DSN-CR-011 §4.4）
 *
 * @author hwyz_leo
 */
public enum AdmissionReason {

    /** 无（仅 ALLOW 使用） **/
    NONE,
    /** uid 在 TSP 设备事实中不存在 **/
    UID_UNKNOWN,
    /** framework 返回的证书 subjectCommonName 缺失或与请求 uid 规范化后不一致 **/
    IDENTITY_MISMATCH,
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
    DEPENDENCY_UNAVAILABLE
}
