package net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * 设备接入准入裁决命令（TSP-DSN-CR-011 §4.2）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAdmissionCheckCmd {

    /** 设备接入身份（证书 CN = HSM UID） **/
    @NotBlank(message = "设备UID不能为空")
    private String uid;

    /** 证书序列号 **/
    @NotBlank(message = "证书序列号不能为空")
    private String certSerial;

    /** 证书 SHA-256 指纹 **/
    @NotBlank(message = "证书指纹不能为空")
    private String certFingerprint;

    /** 本次 mTLS 实际叶子证书（标准 Base64 编码 DER，≤8 KiB） **/
    @NotBlank(message = "叶子证书不能为空")
    private String leafCertificateDer;

    /** MQTT 客户端 ID **/
    private String clientId;

    /** 来源 IP（仅审计摘要） **/
    private String sourceIp;

    /** 请求时刻 **/
    private Instant ts;
}
