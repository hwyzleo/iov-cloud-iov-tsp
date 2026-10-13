package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 设备接入准入裁决领域实体（TSP-DSN-CR-011）
 * <p>
 * 同时承载裁决结果与审计口径：敏感标识（uid/certSerial/sourceIp）只允许以不可逆摘要落库。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAdmission {

    private Long id;

    /** 车辆识别码（未解析时为空） **/
    private String vin;

    /** 裁决结果 **/
    private DeviceAdmissionResult admissionResult;

    /** 裁决原因（语义枚举） **/
    private AdmissionReason reason;

    /** 绑定状态 **/
    private BindStatus bindStatus;

    /** 证书状态 **/
    private CertificateStatus certStatus;

    /** 设备生命周期准入态 **/
    private DeviceStatus deviceStatus;

    /** 绑定版本 **/
    private Long bindVersion;

    /** 事实更新时间：max(binding.lastEventTime, tbox.modifyTime, certificate.checkedAt) **/
    private Instant updatedAt;

    /** 证书检查完成时刻 **/
    private Instant checkedAt;

    /** 吊销状态来源（OCSP/CRL，可为空） **/
    private String revocationSource;

    /** 依赖处置结果（OK / DEPENDENCY_UNAVAILABLE / 具体依赖分类 / 未评估原因） **/
    private String dependencyOutcome;

    /** uid 不可逆摘要（审计） **/
    private String uidDigest;

    /** certSerial 不可逆摘要（审计） **/
    private String certSerialDigest;

    /** sourceIp 不可逆摘要（审计） **/
    private String sourceIpDigest;

    /** 调用方标识 **/
    private String caller;

    /** uid 检查结果（PASS/FAIL，兼容旧审计列 hsm_check_result） **/
    private String uidCheckResult;

    /** 设备状态检查结果（PASS/FAIL，兼容旧审计列 device_status_check_result） **/
    private String deviceStatusCheckResult;

    /** PKI 吊销查询耗时（毫秒） **/
    private Long pkiLatencyMs;

    /** 端到端响应耗时（毫秒） **/
    private Long responseTimeMs;

    private LocalDateTime createTime;
}
