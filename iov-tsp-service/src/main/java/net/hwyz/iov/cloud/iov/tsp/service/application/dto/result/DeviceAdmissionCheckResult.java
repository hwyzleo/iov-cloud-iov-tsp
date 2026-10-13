package net.hwyz.iov.cloud.iov.tsp.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.AdmissionDecision;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.AdmissionReason;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CertificateStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.DeviceStatus;

import java.time.Instant;

/**
 * 设备接入准入裁决结果（TSP-DSN-CR-011 §4.3）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAdmissionCheckResult {

    private AdmissionDecision decision;

    private AdmissionReason reason;

    private String vin;

    private BindStatus bindStatus;

    private CertificateStatus certStatus;

    private DeviceStatus deviceStatus;

    private Long bindVersion;

    private Instant updatedAt;
}
