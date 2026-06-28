package net.hwyz.iov.cloud.iov.tsp.api.vo;

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

    private String certSerial;

    private String certFingerprint;

    private String clientId;

    private String sourceIp;

    private Long ts;
}
