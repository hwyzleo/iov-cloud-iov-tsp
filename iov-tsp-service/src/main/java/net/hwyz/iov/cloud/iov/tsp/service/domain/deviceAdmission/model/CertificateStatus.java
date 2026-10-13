package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model;

/**
 * 证书状态（TSP-DSN-CR-011 §3）
 * <p>
 * {@link #VALID} 仅在吊销查询得到确定 GOOD 且未过期时可判定；UNKNOWN 不等于 VALID。
 *
 * @author hwyz_leo
 */
public enum CertificateStatus {

    /** 确定有效（吊销查询 GOOD 且未过期） **/
    VALID,
    /** 已被吊销 **/
    REVOKED,
    /** 已过期（裁决时刻晚于 notAfter） **/
    EXPIRED,
    /** 未知/依赖不可用 **/
    UNKNOWN
}
