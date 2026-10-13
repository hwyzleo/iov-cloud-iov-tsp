package net.hwyz.iov.cloud.iov.tsp.api.vo;

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
 * 设备接入准入裁决结果VO（TSP-DSN-CR-011 §4.3）
 * <p>
 * ALLOW 时 reason=NONE 且 vin、bindStatus=BOUND、certStatus=VALID、deviceStatus、bindVersion、updatedAt 必须存在；
 * DENY 时尽可能返回已确定状态，不可确定的字段允许为空，但不得伪造 VALID。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAdmissionResultVo {

    /** 裁决结果 **/
    private AdmissionDecision decision;

    /** 裁决原因（语义枚举） **/
    private AdmissionReason reason;

    /** 车辆识别码（ALLOW 必填） **/
    private String vin;

    /** 绑定状态 **/
    private BindStatus bindStatus;

    /** 证书状态 **/
    private CertificateStatus certStatus;

    /** 设备生命周期准入态 **/
    private DeviceStatus deviceStatus;

    /** 绑定版本（ALLOW 必填） **/
    private Long bindVersion;

    /** 事实更新时间：max(binding.lastEventTime, tbox.modifyTime, certificate.checkedAt) **/
    private Instant updatedAt;
}
