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
            log.warn("设备接入准入 PKI 检查缺少叶子证书 DER（mTLS 叶子证书未透传），fail-closed: category={}",
                    CertificateDependencyUnavailableException.CATEGORY_CONFIGURATION);
            throw new CertificateDependencyUnavailableException(
                    CertificateDependencyUnavailableException.CATEGORY_CONFIGURATION,
                    "Leaf certificate DER is required for device admission revocation check", null);
        }
        PkiClient pkiClient = pkiClientProvider.getIfAvailable();
        if (pkiClient == null) {
            log.warn("framework PkiClient 不可用（未装配任何 PkiClient Bean），fail-closed: category={}",
                    CertificateDependencyUnavailableException.CATEGORY_CONFIGURATION);
            throw new CertificateDependencyUnavailableException(
                    CertificateDependencyUnavailableException.CATEGORY_CONFIGURATION,
                    "framework PkiClient is not available", null);
        }
        try {
            PkiClient.CertificateStatusResult result = pkiClient.checkCertificateStatus(
                    new PkiClient.CertificateStatusQuery(
                            identity.leafCertificateDer(),
                            identity.serial(),
                            identity.fingerprint(),
                            decisionAt));
            return new CertificateVerification(
                    mapState(result.revocationState()),
                    result.subjectCommonName(),
                    result.notBefore(),
                    result.notAfter(),
                    result.source() == null ? null : result.source().name(),
                    result.checkedAt());
        } catch (CertificateStatusDependencyUnavailableException e) {
            throw map(CertificateDependencyUnavailableException.CATEGORY_CONNECT, e, pkiClient);
        } catch (CertificateStatusTrustValidationException e) {
            throw map(CertificateDependencyUnavailableException.CATEGORY_TLS, e, pkiClient);
        } catch (CertificateStatusOutcomeUnknownException e) {
            throw map(CertificateDependencyUnavailableException.CATEGORY_TIMEOUT, e, pkiClient);
        } catch (CertificateStatusProtocolException e) {
            throw map(CertificateDependencyUnavailableException.CATEGORY_PROTOCOL, e, pkiClient);
        } catch (CertificateStatusConfigurationException e) {
            throw map(CertificateDependencyUnavailableException.CATEGORY_CONFIGURATION, e, pkiClient);
        } catch (RuntimeException e) {
            throw map(CertificateDependencyUnavailableException.CATEGORY_INTERNAL, e, pkiClient);
        }
    }

    /**
     * 统一映射并记录 framework 类型化异常；此处携带最原始的 framework 异常信息与 PkiClient 实现类型，
     * 便于区分「能力未启用 / Bean 未装配 / 配置非法 / 网络失败」。
     */
    private static CertificateDependencyUnavailableException map(String category, RuntimeException cause,
                                                                 PkiClient pkiClient) {
        log.warn("PKI 证书状态查询失败，fail-closed: category={}, pkiClient={}, exception={}, message={}, rootCause={}",
                category,
                pkiClient == null ? null : pkiClient.getClass().getSimpleName(),
                cause.getClass().getSimpleName(),
                cause.getMessage(),
                rootCauseSummary(cause));
        return new CertificateDependencyUnavailableException(
                category, "PKI certificate status query failed: " + cause.getClass().getSimpleName(), cause);
    }

    /**
     * 追到最内层 cause 并生成 "类型: 消息" 摘要（带深度上限防循环）。
     */
    private static String rootCauseSummary(Throwable throwable) {
        Throwable root = throwable;
        int depth = 0;
        while (root.getCause() != null && root.getCause() != root && depth < 16) {
            root = root.getCause();
            depth++;
        }
        return root.getClass().getSimpleName() + (root.getMessage() == null ? "" : ": " + root.getMessage());
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
