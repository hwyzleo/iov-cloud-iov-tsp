package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.CertificateIdentity;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.CertificateVerification;

import java.time.Instant;

/**
 * 证书吊销验证仓储接口（TSP-DSN-CR-011 §3）
 * <p>
 * 领域端口：只接收证书身份与裁决时刻，返回统一证书状态；实现由 Infrastructure 适配
 * framework-security 的 PkiClient（OCSP 优先 / CRL 后备），所有依赖异常映射为
 * {@code CertificateDependencyUnavailableException} 并保留 cause。
 *
 * @author hwyz_leo
 */
public interface CertificateVerificationRepository {

    /**
     * 实时查询证书吊销状态（不缓存）
     *
     * @param identity   证书身份（serial / fingerprint / 本次 mTLS 叶子证书 DER）
     * @param decisionAt 裁决时刻（用于有效期冗余兜底与 OCSP/CRL 时效校验）
     * @return 证书验证结果；UNKNOWN 不等于 VALID
     */
    CertificateVerification verify(CertificateIdentity identity, Instant decisionAt);
}
