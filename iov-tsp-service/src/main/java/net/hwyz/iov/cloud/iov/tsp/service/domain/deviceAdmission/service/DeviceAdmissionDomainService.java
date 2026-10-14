package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.CertificateDependencyUnavailableException;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.DigestUtils;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.DeviceUidNormalizer;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.AdmissionReason;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.BindStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.CertificateIdentity;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.CertificateStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.CertificateVerification;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmission;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmissionCommand;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmissionResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.CertificateVerificationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.DeviceAdmissionLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.DeviceAdmissionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 设备接入鉴权领域服务（TSP-DSN-CR-011）
 * <p>
 * 编排顺序固定为：UID → 绑定 → 证书吊销/身份/有效期 → 设备生命周期态 → 裁决。
 * 安全结论：任何无法获得确定 VALID/REVOKED 的 PKI 依赖结果一律 fail-closed
 * （DENY + DEPENDENCY_UNAVAILABLE），不得降级为 ALLOW。
 * <p>
 * 证书身份：framework 返回的 {@code subjectCommonName} 必须经统一 {@link DeviceUidNormalizer}
 * 与请求 uid 比较，缺失或不一致返回 DENY + IDENTITY_MISMATCH，不得归类为 CERT_REVOKED 或
 * DEPENDENCY_UNAVAILABLE。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceAdmissionDomainService {

    private static final Set<DeviceStatus> ADMISSION_ALLOWED_STATUSES = Set.of(
            DeviceStatus.PRE_ACTIVE,
            DeviceStatus.ACTIVATING,
            DeviceStatus.ACTIVE
    );

    private static final String CHECK_PASS = "PASS";
    private static final String CHECK_FAIL = "FAIL";
    private static final String OUTCOME_OK = "OK";
    private static final String OUTCOME_DEVICE_STATUS_INVALID = "DEVICE_STATUS_INVALID";

    private final DeviceAdmissionRepository deviceAdmissionRepository;
    private final DeviceAdmissionLogRepository deviceAdmissionLogRepository;
    private final CertificateVerificationRepository certificateVerificationRepository;

    /**
     * 执行设备接入准入裁决
     *
     * @param command 裁决命令（必须携带本次 mTLS 叶子证书 DER）
     * @return 裁决结果（已落审计）
     */
    public DeviceAdmission decide(DeviceAdmissionCommand command) {
        long startTime = System.currentTimeMillis();
        Instant decisionAt = command.occurredAt() == null ? Instant.now() : command.occurredAt();
        String uidDigest = DigestUtils.sha256Hex(command.uid());
        String certSerialDigest = DigestUtils.sha256Hex(command.certSerial());
        String sourceIpDigest = DigestUtils.sha256Hex(command.sourceIp());

        DeviceAdmission admission = evaluate(command, decisionAt);
        admission.setUidDigest(uidDigest);
        admission.setCertSerialDigest(certSerialDigest);
        admission.setSourceIpDigest(sourceIpDigest);
        admission.setCaller(command.caller());
        admission.setResponseTimeMs(System.currentTimeMillis() - startTime);
        admission.setCreateTime(LocalDateTime.now());

        deviceAdmissionLogRepository.save(admission);

        log.info("设备接入准入裁决完成: uidDigest={}, decision={}, reason={}, certStatus={}, time={}ms",
                uidDigest, admission.getAdmissionResult().getCode(), admission.getReason(),
                admission.getCertStatus(), admission.getResponseTimeMs());

        return admission;
    }

    /**
     * 按固定顺序执行各项判定；任一项失败立即 fail-closed 返回。
     */
    private DeviceAdmission evaluate(DeviceAdmissionCommand command, Instant decisionAt) {
        // 1. UID 必须存在（不存在不调用 PKI）
        String uid = DeviceUidNormalizer.normalize(command.uid());
        Optional<DeviceAdmissionRepository.TboxInfo> tboxOptional = uid == null
                ? Optional.empty()
                : deviceAdmissionRepository.findByHsm(uid);
        if (tboxOptional.isEmpty()) {
            return deny(AdmissionReason.UID_UNKNOWN, null)
                    .bindStatus(BindStatus.UNKNOWN)
                    .uidCheckResult(CHECK_FAIL)
                    .dependencyOutcome(AdmissionReason.UID_UNKNOWN.name())
                    .build();
        }
        DeviceAdmissionRepository.TboxInfo tbox = tboxOptional.get();

        // 2. 绑定必须唯一 ACTIVE（未绑定/冲突不调用 PKI）
        List<DeviceAdmissionRepository.BindingInfo> bindings =
                deviceAdmissionRepository.findActiveBindingsBySn(tbox.sn());
        if (bindings.isEmpty()) {
            return deny(AdmissionReason.UNBOUND, null)
                    .bindStatus(BindStatus.UNBOUND)
                    .uidCheckResult(CHECK_PASS)
                    .dependencyOutcome(AdmissionReason.UNBOUND.name())
                    .build();
        }
        if (bindings.size() > 1) {
            return deny(AdmissionReason.BINDING_CONFLICT, null)
                    .bindStatus(BindStatus.CONFLICT)
                    .uidCheckResult(CHECK_PASS)
                    .dependencyOutcome(AdmissionReason.BINDING_CONFLICT.name())
                    .build();
        }
        DeviceAdmissionRepository.BindingInfo binding = bindings.get(0);

        // 3. 证书吊销 + 有效期冗余兜底
        long pkiStart = System.currentTimeMillis();
        CertificateVerification verification;
        try {
            verification = certificateVerificationRepository.verify(
                    new CertificateIdentity(command.certSerial(), command.certFingerprint(), command.leafCertificateDer()),
                    decisionAt);
        } catch (CertificateDependencyUnavailableException e) {
            log.warn("PKI 证书状态查询依赖不可用，fail-closed: uidDigest={}, category={}, message={}, cause={}",
                    DigestUtils.sha256Hex(command.uid()), e.getCategory(), e.getMessage(), causeChain(e));
            return deny(AdmissionReason.DEPENDENCY_UNAVAILABLE, null)
                    .bindStatus(BindStatus.BOUND)
                    .certStatus(CertificateStatus.UNKNOWN)
                    .uidCheckResult(CHECK_PASS)
                    .pkiLatencyMs(System.currentTimeMillis() - pkiStart)
                    .dependencyOutcome(e.getCategory())
                    .build();
        }
        long pkiLatencyMs = System.currentTimeMillis() - pkiStart;

        if (verification.status() == CertificateStatus.REVOKED) {
            return deny(AdmissionReason.CERT_REVOKED, binding)
                    .certStatus(CertificateStatus.REVOKED)
                    .checkedAt(verification.checkedAt())
                    .revocationSource(verification.source())
                    .uidCheckResult(CHECK_PASS)
                    .pkiLatencyMs(pkiLatencyMs)
                    .dependencyOutcome(OUTCOME_OK)
                    .build();
        }
        if (verification.status() == CertificateStatus.UNKNOWN) {
            return deny(AdmissionReason.DEPENDENCY_UNAVAILABLE, binding)
                    .certStatus(CertificateStatus.UNKNOWN)
                    .checkedAt(verification.checkedAt())
                    .revocationSource(verification.source())
                    .uidCheckResult(CHECK_PASS)
                    .pkiLatencyMs(pkiLatencyMs)
                    .dependencyOutcome(CertificateDependencyUnavailableException.CATEGORY_UNKNOWN)
                    .build();
        }
        // 3.1 证书身份：framework 仅返回证书事实，CN↔uid 业务比较归 TSP（TSP-DSN-CR-011 §3）
        if (!DeviceUidNormalizer.matches(uid, verification.subjectCommonName())) {
            log.warn("设备接入证书身份不一致，fail-closed: uidDigest={}, pkiLatencyMs={}",
                    DigestUtils.sha256Hex(command.uid()), pkiLatencyMs);
            return deny(AdmissionReason.IDENTITY_MISMATCH, binding)
                    .certStatus(CertificateStatus.VALID)
                    .checkedAt(verification.checkedAt())
                    .revocationSource(verification.source())
                    .uidCheckResult(CHECK_FAIL)
                    .pkiLatencyMs(pkiLatencyMs)
                    .dependencyOutcome(AdmissionReason.IDENTITY_MISMATCH.name())
                    .build();
        }
        if (verification.notAfter() != null && decisionAt.isAfter(verification.notAfter())) {
            return deny(AdmissionReason.CERT_EXPIRED, binding)
                    .certStatus(CertificateStatus.EXPIRED)
                    .checkedAt(verification.checkedAt())
                    .revocationSource(verification.source())
                    .uidCheckResult(CHECK_PASS)
                    .pkiLatencyMs(pkiLatencyMs)
                    .dependencyOutcome(OUTCOME_OK)
                    .build();
        }

        // 4. 设备生命周期准入态（不得被 PKI 结果覆盖为 ALLOW）
        DeviceStatus deviceStatus = mapDeviceStatus(tbox.deviceStatus());
        if (deviceStatus == DeviceStatus.BLOCKED) {
            return deny(AdmissionReason.DEVICE_BLOCKED, binding)
                    .deviceStatus(DeviceStatus.BLOCKED)
                    .certStatus(CertificateStatus.VALID)
                    .checkedAt(verification.checkedAt())
                    .revocationSource(verification.source())
                    .uidCheckResult(CHECK_PASS)
                    .deviceStatusCheckResult(CHECK_FAIL)
                    .pkiLatencyMs(pkiLatencyMs)
                    .dependencyOutcome(OUTCOME_OK)
                    .build();
        }
        if (deviceStatus == DeviceStatus.RETIRED) {
            return deny(AdmissionReason.DEVICE_RETIRED, binding)
                    .deviceStatus(DeviceStatus.RETIRED)
                    .certStatus(CertificateStatus.VALID)
                    .checkedAt(verification.checkedAt())
                    .revocationSource(verification.source())
                    .uidCheckResult(CHECK_PASS)
                    .deviceStatusCheckResult(CHECK_FAIL)
                    .pkiLatencyMs(pkiLatencyMs)
                    .dependencyOutcome(OUTCOME_OK)
                    .build();
        }
        if (deviceStatus == null || !ADMISSION_ALLOWED_STATUSES.contains(deviceStatus)) {
            return deny(AdmissionReason.DEPENDENCY_UNAVAILABLE, binding)
                    .certStatus(CertificateStatus.VALID)
                    .checkedAt(verification.checkedAt())
                    .revocationSource(verification.source())
                    .uidCheckResult(CHECK_PASS)
                    .deviceStatusCheckResult(CHECK_FAIL)
                    .pkiLatencyMs(pkiLatencyMs)
                    .dependencyOutcome(OUTCOME_DEVICE_STATUS_INVALID)
                    .build();
        }

        // 5. 全部通过
        Instant updatedAt = maxInstant(binding.lastEventTime(), tbox.modifyTime(), verification.checkedAt());
        return DeviceAdmission.builder()
                .vin(binding.vin())
                .admissionResult(DeviceAdmissionResult.ALLOW)
                .reason(AdmissionReason.NONE)
                .bindStatus(BindStatus.BOUND)
                .certStatus(CertificateStatus.VALID)
                .deviceStatus(deviceStatus)
                .bindVersion(binding.bindingVersion())
                .updatedAt(updatedAt)
                .checkedAt(verification.checkedAt())
                .revocationSource(verification.source())
                .uidCheckResult(CHECK_PASS)
                .deviceStatusCheckResult(CHECK_PASS)
                .pkiLatencyMs(pkiLatencyMs)
                .dependencyOutcome(OUTCOME_OK)
                .build();
    }

    private DeviceAdmission.DeviceAdmissionBuilder deny(AdmissionReason reason, DeviceAdmissionRepository.BindingInfo binding) {
        DeviceAdmission.DeviceAdmissionBuilder builder = DeviceAdmission.builder()
                .admissionResult(DeviceAdmissionResult.DENY)
                .reason(reason);
        if (binding != null) {
            builder.vin(binding.vin());
            builder.bindVersion(binding.bindingVersion());
        }
        return builder;
    }

    /**
     * 设备状态码映射；未知/空返回 null 由调用方 fail-closed 处理。
     */
    private static DeviceStatus mapDeviceStatus(Integer code) {
        if (code == null) {
            return null;
        }
        try {
            return DeviceStatus.fromCode(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Instant maxInstant(Instant... values) {
        Instant max = null;
        for (Instant value : values) {
            if (value != null && (max == null || value.isAfter(max))) {
                max = value;
            }
        }
        return max;
    }

    /**
     * 展开 cause 链生成 "类型: 消息" 摘要，用于定位 framework 抛出的原始配置/依赖异常（带深度上限防循环）。
     */
    private static String causeChain(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable.getCause();
        int depth = 0;
        while (current != null && current.getCause() != current && depth < 16) {
            if (builder.length() > 0) {
                builder.append(" <- ");
            }
            builder.append(current.getClass().getSimpleName());
            if (current.getMessage() != null) {
                builder.append(": ").append(current.getMessage());
            }
            current = current.getCause();
            depth++;
        }
        return builder.length() == 0 ? null : builder.toString();
    }
}
