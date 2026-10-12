package net.hwyz.iov.cloud.iov.tsp.api.vo.mpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxBindingStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MPT TBOX 列表摘要
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptTboxSummaryVo {

    private String sn;

    private String vin;

    private String partCode;

    /**
     * 设备状态码（DeviceStatus.code）
     */
    private Integer deviceStatus;

    private LocalDateTime firstLoginTime;

    private LocalDateTime lastLoginTime;

    private LocalDateTime activateTime;

    private TboxBindingStatus bindingStatus;

    private Long bindingVersion;

    private List<TboxAnomalyTagVo> anomalyTags;

    /**
     * 激活耗时（秒）：firstLoginTime -> activateTime
     */
    private Long activationDurationSeconds;

    /**
     * 离线时长（秒）：now - lastLoginTime
     */
    private Long lastLoginAgeSeconds;

    /**
     * 脱敏 HSM UID（前4后4）
     */
    private String maskedHsmUid;

    /**
     * 脱敏 IMEI（前4后4）
     */
    private String maskedImei;

    /**
     * 脱敏 ICCID 列表（前6后4）
     */
    private List<String> maskedIccids;

    private String supplierCode;

    private String hardwareVersion;

    private String softwareVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
