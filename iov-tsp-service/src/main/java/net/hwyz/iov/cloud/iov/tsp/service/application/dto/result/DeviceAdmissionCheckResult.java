package net.hwyz.iov.cloud.iov.tsp.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备接入鉴权检查结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAdmissionCheckResult {

    private String admission;
    private String reason;
    private String vin;
    private String bindStatus;
    private String certStatus;
    private String deviceStatus;
    private String bindVersion;
    private String updatedAt;
    private CheckDetails checkDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckDetails {
        private CheckResult hsmCheck;
        private CheckResult pkiCheck;
        private CheckResult deviceStatusCheck;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckResult {
        private String status;
        private String message;
    }
}
