package net.hwyz.iov.cloud.iov.tsp.api.service.deviceAdmission.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备接入鉴权检查结果VO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAdmissionResultVo {

    private String admission;
    private String reason;
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
