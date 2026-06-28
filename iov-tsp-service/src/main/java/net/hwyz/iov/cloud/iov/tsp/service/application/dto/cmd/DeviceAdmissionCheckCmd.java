package net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 设备接入鉴权检查命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAdmissionCheckCmd {

    @NotBlank(message = "设备HSM标识不能为空")
    private String hsm;

    @NotBlank(message = "车辆VIN不能为空")
    private String vin;

    private String certSerial;

    private String certFingerprint;

    private String clientId;

    private String sourceIp;

    private Long ts;
}
