package net.hwyz.iov.cloud.iov.tsp.service.application.assembler;

import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.DeviceAdmissionCheckResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmission;
import org.springframework.stereotype.Component;

/**
 * 设备接入鉴权装配器
 */
@Component
public class DeviceAdmissionAssembler {

    /**
     * 领域实体转结果DTO
     */
    public DeviceAdmissionCheckResult toResult(DeviceAdmission deviceAdmission) {
        return DeviceAdmissionCheckResult.builder()
            .admission(deviceAdmission.getAdmissionResult().getCode())
            .reason(deviceAdmission.getReason())
            .checkDetails(DeviceAdmissionCheckResult.CheckDetails.builder()
                .hsmCheck(toCheckResult(deviceAdmission.getHsmCheckResult()))
                .pkiCheck(toCheckResult(deviceAdmission.getPkiCheckResult()))
                .deviceStatusCheck(toCheckResult(deviceAdmission.getDeviceStatusCheckResult()))
                .build())
            .build();
    }

    private DeviceAdmissionCheckResult.CheckResult toCheckResult(DeviceAdmission.CheckResult checkResult) {
        if (checkResult == null) {
            return null;
        }

        return DeviceAdmissionCheckResult.CheckResult.builder()
            .status(checkResult.getStatus().getCode())
            .message(checkResult.getMessage())
            .build();
    }
}
