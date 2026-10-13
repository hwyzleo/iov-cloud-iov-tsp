package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.application.assembler.DeviceAdmissionAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.DeviceAdmissionCheckCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.DeviceAdmissionCheckResult;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.CertificateDependencyUnavailableException;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.DeviceAdmissionParamInvalidException;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmission;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmissionCommand;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.service.DeviceAdmissionDomainService;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.metrics.DeviceAdmissionMetrics;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * 设备接入准入裁决应用服务（TSP-DSN-CR-011）
 * <p>
 * 负责请求参数校验（Base64 DER 解码 + 8 KiB 上限）、调用领域服务并记录指标；
 * 指标与审计均不得包含 uid/certSerial/sourceIp 明文。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceAdmissionAppService {

    /** 叶子证书 DER 解码后上限（8 KiB） **/
    private static final int MAX_LEAF_CERT_DER_BYTES = 8 * 1024;

    private final DeviceAdmissionDomainService deviceAdmissionDomainService;
    private final DeviceAdmissionMetrics deviceAdmissionMetrics;

    /**
     * 执行设备接入准入裁决
     *
     * @param cmd    裁决命令
     * @param caller 调用方标识（审计）
     * @return 裁决结果
     */
    public DeviceAdmissionCheckResult decide(DeviceAdmissionCheckCmd cmd, String caller) {
        byte[] leafCertificateDer = decodeLeafCertificate(cmd.getLeafCertificateDer());

        DeviceAdmissionCommand command = new DeviceAdmissionCommand(
                cmd.getUid(),
                cmd.getCertSerial(),
                cmd.getCertFingerprint(),
                leafCertificateDer,
                cmd.getClientId(),
                cmd.getSourceIp(),
                caller,
                cmd.getTs());

        long startTime = System.currentTimeMillis();
        DeviceAdmission admission = deviceAdmissionDomainService.decide(command);
        long latencyMs = System.currentTimeMillis() - startTime;

        recordMetrics(admission, latencyMs);

        return DeviceAdmissionAssembler.INSTANCE.toResult(admission);
    }

    private void recordMetrics(DeviceAdmission admission, long latencyMs) {
        deviceAdmissionMetrics.decision(
                admission.getAdmissionResult().getCode(),
                admission.getReason() == null ? null : admission.getReason().name());
        deviceAdmissionMetrics.latency(latencyMs);

        boolean pkiAttempted = admission.getPkiLatencyMs() != null;
        String source = admission.getRevocationSource() == null ? "UNKNOWN" : admission.getRevocationSource();
        if (pkiAttempted) {
            String outcome = admission.getCertStatus() == null ? "UNKNOWN" : admission.getCertStatus().name();
            deviceAdmissionMetrics.pkiQuery(source, outcome);
            deviceAdmissionMetrics.pkiLatency(source, admission.getPkiLatencyMs());
        }
        if (admission.getCertStatus() == net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.CertificateStatus.REVOKED) {
            deviceAdmissionMetrics.revoked();
        }
        if (admission.getReason() == net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.AdmissionReason.DEPENDENCY_UNAVAILABLE
                && isPkiDependencyCategory(admission.getDependencyOutcome())) {
            deviceAdmissionMetrics.dependencyUnavailable(admission.getDependencyOutcome());
        }
    }

    /**
     * 只有 PKI 依赖分类才计入 {@code tsp_pki_dependency_unavailable_total}，
     * 避免把设备状态数据异常等非 PKI 失败混入安全告警指标。
     */
    private static boolean isPkiDependencyCategory(String category) {
        if (category == null) {
            return false;
        }
        return switch (category) {
            case CertificateDependencyUnavailableException.CATEGORY_DNS,
                 CertificateDependencyUnavailableException.CATEGORY_CONNECT,
                 CertificateDependencyUnavailableException.CATEGORY_TLS,
                 CertificateDependencyUnavailableException.CATEGORY_TIMEOUT,
                 CertificateDependencyUnavailableException.CATEGORY_PROTOCOL,
                 CertificateDependencyUnavailableException.CATEGORY_CONFIGURATION,
                 CertificateDependencyUnavailableException.CATEGORY_UNKNOWN,
                 CertificateDependencyUnavailableException.CATEGORY_INTERNAL -> true;
            default -> false;
        };
    }

    private static byte[] decodeLeafCertificate(String base64Der) {
        if (base64Der == null || base64Der.isBlank()) {
            throw new DeviceAdmissionParamInvalidException("叶子证书不能为空");
        }
        byte[] der;
        try {
            der = Base64.getDecoder().decode(base64Der);
        } catch (IllegalArgumentException e) {
            throw new DeviceAdmissionParamInvalidException("叶子证书必须为标准 Base64 编码 DER");
        }
        if (der.length == 0) {
            throw new DeviceAdmissionParamInvalidException("叶子证书不能为空");
        }
        if (der.length > MAX_LEAF_CERT_DER_BYTES) {
            throw new DeviceAdmissionParamInvalidException("叶子证书超过 8 KiB 上限");
        }
        return der;
    }
}
