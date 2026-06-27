package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.CheckStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmission;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmissionResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.DeviceAdmissionLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.DeviceAdmissionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.CertificateVerificationRepository;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 设备接入鉴权领域服务
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceAdmissionDomainService {

    private final DeviceAdmissionRepository deviceAdmissionRepository;
    private final DeviceAdmissionLogRepository deviceAdmissionLogRepository;
    private final CertificateVerificationRepository certificateVerificationRepository;

    /**
     * 执行设备接入鉴权检查
     * @param hsm 设备HSM标识
     * @param vin 车辆VIN
     * @return 设备接入鉴权结果
     */
    public DeviceAdmission checkAdmission(String hsm, String vin) {
        long startTime = System.currentTimeMillis();

        CompletableFuture<DeviceAdmission.CheckResult> hsmCheckFuture =
            CompletableFuture.supplyAsync(() -> checkHsmBinding(hsm, vin));

        CompletableFuture<DeviceAdmission.CheckResult> pkiCheckFuture =
            CompletableFuture.supplyAsync(() -> checkPkiRevocation(hsm));

        CompletableFuture<DeviceAdmission.CheckResult> deviceStatusCheckFuture =
            CompletableFuture.supplyAsync(() -> checkDeviceStatus(hsm));

        CompletableFuture.allOf(hsmCheckFuture, pkiCheckFuture, deviceStatusCheckFuture).join();

        DeviceAdmission.CheckResult hsmCheckResult = hsmCheckFuture.join();
        DeviceAdmission.CheckResult pkiCheckResult = pkiCheckFuture.join();
        DeviceAdmission.CheckResult deviceStatusCheckResult = deviceStatusCheckFuture.join();

        DeviceAdmissionResult admissionResult = aggregateResults(hsmCheckResult, pkiCheckResult, deviceStatusCheckResult);
        String reason = generateReason(admissionResult, hsmCheckResult, pkiCheckResult, deviceStatusCheckResult);

        long responseTimeMs = System.currentTimeMillis() - startTime;

        DeviceAdmission deviceAdmission = DeviceAdmission.builder()
            .vin(vin)
            .hsm(hsm)
            .admissionResult(admissionResult)
            .reason(reason)
            .hsmCheckResult(hsmCheckResult)
            .pkiCheckResult(pkiCheckResult)
            .deviceStatusCheckResult(deviceStatusCheckResult)
            .responseTimeMs(responseTimeMs)
            .build();

        deviceAdmissionLogRepository.save(deviceAdmission);

        log.info("设备接入鉴权检查完成: hsm={}, vin={}, result={}, reason={}, time={}ms",
            hsm, vin, admissionResult.getCode(), reason, responseTimeMs);

        return deviceAdmission;
    }

    private DeviceAdmission.CheckResult checkHsmBinding(String hsm, String vin) {
        try {
            var tboxInfo = deviceAdmissionRepository.findByHsm(hsm);

            if (tboxInfo.isEmpty()) {
                return DeviceAdmission.CheckResult.builder()
                    .status(CheckStatus.FAIL)
                    .message("HSM绑定关系未找到")
                    .build();
            }

            if (!tboxInfo.get().vin().equals(vin)) {
                return DeviceAdmission.CheckResult.builder()
                    .status(CheckStatus.FAIL)
                    .message("HSM与VIN不匹配")
                    .build();
            }

            return DeviceAdmission.CheckResult.builder()
                .status(CheckStatus.PASS)
                .message("HSM绑定检查通过")
                .build();
        } catch (Exception e) {
            log.error("HSM绑定检查异常", e);
            return DeviceAdmission.CheckResult.builder()
                .status(CheckStatus.ERROR)
                .message("HSM绑定检查异常: " + e.getMessage())
                .build();
        }
    }

    private DeviceAdmission.CheckResult checkPkiRevocation(String hsm) {
        try {
            boolean isRevoked = certificateVerificationRepository.checkRevocation(hsm);

            if (isRevoked) {
                return DeviceAdmission.CheckResult.builder()
                    .status(CheckStatus.FAIL)
                    .message("证书已被吊销")
                    .build();
            }

            return DeviceAdmission.CheckResult.builder()
                .status(CheckStatus.PASS)
                .message("PKI吊销检查通过")
                .build();
        } catch (Exception e) {
            log.warn("PKI服务不可用，允许接入", e);
            return DeviceAdmission.CheckResult.builder()
                .status(CheckStatus.PASS)
                .message("PKI服务不可用，允许接入")
                .build();
        }
    }

    private DeviceAdmission.CheckResult checkDeviceStatus(String hsm) {
        try {
            var tboxInfo = deviceAdmissionRepository.findByHsm(hsm);

            if (tboxInfo.isEmpty()) {
                return DeviceAdmission.CheckResult.builder()
                    .status(CheckStatus.FAIL)
                    .message("设备未找到")
                    .build();
            }

            DeviceStatus status = DeviceStatus.fromCode(tboxInfo.get().deviceStatus());

            if (status != DeviceStatus.ACTIVE) {
                return DeviceAdmission.CheckResult.builder()
                    .status(CheckStatus.FAIL)
                    .message("设备状态无效: " + status.getDesc())
                    .build();
            }

            return DeviceAdmission.CheckResult.builder()
                .status(CheckStatus.PASS)
                .message("设备状态检查通过")
                .build();
        } catch (Exception e) {
            log.error("设备状态检查异常", e);
            return DeviceAdmission.CheckResult.builder()
                .status(CheckStatus.ERROR)
                .message("设备状态检查异常: " + e.getMessage())
                .build();
        }
    }

    private DeviceAdmissionResult aggregateResults(
            DeviceAdmission.CheckResult hsmCheck,
            DeviceAdmission.CheckResult pkiCheck,
            DeviceAdmission.CheckResult deviceStatusCheck) {

        if (hsmCheck.getStatus() == CheckStatus.FAIL ||
            hsmCheck.getStatus() == CheckStatus.ERROR) {
            return DeviceAdmissionResult.DENY;
        }

        if (pkiCheck.getStatus() == CheckStatus.FAIL) {
            return DeviceAdmissionResult.DENY;
        }

        if (deviceStatusCheck.getStatus() == CheckStatus.FAIL ||
            deviceStatusCheck.getStatus() == CheckStatus.ERROR) {
            return DeviceAdmissionResult.DENY;
        }

        return DeviceAdmissionResult.ALLOW;
    }

    private String generateReason(
            DeviceAdmissionResult result,
            DeviceAdmission.CheckResult hsmCheck,
            DeviceAdmission.CheckResult pkiCheck,
            DeviceAdmission.CheckResult deviceStatusCheck) {

        if (result == DeviceAdmissionResult.ALLOW) {
            return "所有检查通过";
        }

        StringBuilder reason = new StringBuilder();

        if (hsmCheck.getStatus() != CheckStatus.PASS) {
            reason.append("HSM绑定检查: ").append(hsmCheck.getMessage()).append("; ");
        }

        if (pkiCheck.getStatus() != CheckStatus.PASS) {
            reason.append("PKI吊销检查: ").append(pkiCheck.getMessage()).append("; ");
        }

        if (deviceStatusCheck.getStatus() != CheckStatus.PASS) {
            reason.append("设备状态检查: ").append(deviceStatusCheck.getMessage()).append("; ");
        }

        return reason.toString().trim();
    }
}
