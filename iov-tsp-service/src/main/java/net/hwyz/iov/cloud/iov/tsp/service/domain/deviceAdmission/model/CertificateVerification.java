package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model;

import java.time.Instant;

/**
 * 证书吊销验证结果（TSP-DSN-CR-011 §3）
 *
 * @param status            证书状态（VALID/REVOKED/EXPIRED/UNKNOWN）
 * @param subjectCommonName framework 从本次实际叶子证书解析的 Subject CN（可能为空，由 TSP 完成 CN↔uid 业务校验）
 * @param notBefore         证书有效期开始
 * @param notAfter          证书有效期结束（供 TSP 冗余兜底）
 * @param source            吊销状态来源（OCSP/CRL，可为空）
 * @param checkedAt         检查完成时刻
 * @author hwyz_leo
 */
public record CertificateVerification(
        CertificateStatus status,
        String subjectCommonName,
        Instant notBefore,
        Instant notAfter,
        String source,
        Instant checkedAt) {
}
