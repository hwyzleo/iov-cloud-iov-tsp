package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model;

/**
 * 证书身份（TSP-DSN-CR-011 §3）
 * <p>
 * 叶子证书 DER 为敏感材料：仅用于本次裁决，防御性拷贝避免外部持有引用；
 * 领域层/基础设施不得持久化或写入日志。
 *
 * @param serial             证书序列号（provider canonical form）
 * @param fingerprint        证书 SHA-256 指纹
 * @param leafCertificateDer 本次 mTLS 实际叶子证书 DER
 * @author hwyz_leo
 */
public record CertificateIdentity(String serial, String fingerprint, byte[] leafCertificateDer) {

    public CertificateIdentity {
        leafCertificateDer = leafCertificateDer == null ? null : leafCertificateDer.clone();
    }

    @Override
    public byte[] leafCertificateDer() {
        return leafCertificateDer == null ? null : leafCertificateDer.clone();
    }
}
