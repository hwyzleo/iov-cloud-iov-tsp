package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备接入鉴权领域实体
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAdmission {

    private Long id;
    private String vin;
    private String hsm;
    private DeviceAdmissionResult admissionResult;
    private String reason;
    private CheckResult hsmCheckResult;
    private CheckResult pkiCheckResult;
    private CheckResult deviceStatusCheckResult;
    private Long responseTimeMs;
    private LocalDateTime createTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckResult {
        private CheckStatus status;
        private String message;
        private String vin;
    }
}
