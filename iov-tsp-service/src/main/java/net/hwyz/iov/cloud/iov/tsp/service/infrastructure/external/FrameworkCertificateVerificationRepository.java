package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.security.crypto.client.PkiClient;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusConfigurationException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusDependencyUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusOutcomeUnknownException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusProtocolException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusTrustValidationException;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.CertificateDependencyUnavailableException;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.CertificateIdentity;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.CertificateStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.CertificateVerification;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.CertificateVerificationRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * 证书吊销验证仓储实现：适配 framework-security PkiClient（TSP-DSN-CR-011 §3/§10）
 * <p>
 * 仅把领域请求映射到 framework 的 {@code PkiClient.checkCertificateStatus}（OCSP 优先 / CRL 后备，
 * TLS/超时/重试与协议解析均在 framework 内完成），并把 framework 类型化异常统一映射为
 * {@link CertificateDependencyUnavailableException} 且保留 cause —— 绝不返回 VALID 兜底。
 * <p>
 * 叶子证书为敏感材料，不落库、不写日志。
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FrameworkCertificateVerificationRepository implements CertificateVerificationRepository {

    private final ObjectProvider<PkiClient> pkiClientProvider;

    @Override
    public CertificateVerification verify(CertificateIdentity identity, Instant decisionAt) {
        if (identity == null || identity.leafCertificateDer() == null || identity.leafCertificateDer().length == 0) {
            throw new CertificateDependencyUnavailableException(
                    CertificateDependencyUnavailableException.CATEGORY_CONFIGURATION,
                    "Leaf certificate DER is required for device admission revocation check", null);
        }
        PkiClient pkiClient = pkiClientProvider.getIfAvailable();
        if (pkiClient == null) {
            throw new CertificateDependencyUnavailableException(
                    CertificateDependencyUnavailableException.CATEGORY_CONFIGURATION,
                    "framework PkiClient is not available", null);
        }
        try {
            PkiClient.CertificateStatusResult result = pkiClient.checkCertificateStatus(
                    new PkiClient.CertificateStatusQuery(
                            identity.serial(),
                            identity.fingerprint(),
                            identity.leafCertificateDer(),
                            decisionAt));
            return new CertificateVerification(
                    mapState(result.revocationState()),
                    result.notBefore(),
                    result.notAfter(),
                    result.source() == null ? null : result.source().name(),
                    result.checkedAt());
        } catch (CertificateStatusDependencyUnavailableException e) {
            throw map(CertificateDependencyUnavailableException.CATEGORY_CONNECT, e);
        } catch (CertificateStatusTrustValidationException e) {
            throw map(CertificateDependencyUnavailableException.CATEGORY_TLS, e);
        } catch (CertificateStatusOutcomeUnknownException e) {
            throw map(CertificateDependencyUnavailableException.CATEGORY_TIMEOUT, e);
        } catch (CertificateStatusProtocolException e) {
            throw map(CertificateDependencyUnavailableException.CATEGORY_PROTOCOL, e);
        } catch (CertificateStatusConfigurationException e) {
            throw map(CertificateDependencyUnavailableException.CATEGORY_CONFIGURATION, e);
        } catch (RuntimeException e) {
            throw map(CertificateDependencyUnavailableException.CATEGORY_INTERNAL, e);
        }
    }

    private static CertificateDependencyUnavailableException map(String category, RuntimeException cause) {
        return new CertificateDependencyUnavailableException(
                category, "PKI certificate status query failed: " + cause.getClass().getSimpleName(), cause);
    }

    private static CertificateStatus mapState(PkiClient.RevocationState state) {
        if (state == null) {
            return CertificateStatus.UNKNOWN;
        }
        return switch (state) {
            case GOOD -> CertificateStatus.VALID;
            case REVOKED -> CertificateStatus.REVOKED;
            case UNKNOWN -> CertificateStatus.UNKNOWN;
        };
    }
}
