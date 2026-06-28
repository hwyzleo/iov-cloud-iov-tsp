package net.hwyz.iov.cloud.iov.tsp.api.service.deviceAdmission.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 设备接入鉴权检查请求VO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAdmissionCheckVo {

    @NotBlank(message = "设备HSM标识不能为空")
    private String hsm;

    @NotBlank(message = "车辆VIN不能为空")
    private String vin;
}
