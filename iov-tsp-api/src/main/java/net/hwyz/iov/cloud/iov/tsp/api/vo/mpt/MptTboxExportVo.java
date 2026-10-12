package net.hwyz.iov.cloud.iov.tsp.api.vo.mpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.framework.common.annotation.Excel;

import java.time.LocalDateTime;

/**
 * MPT TBOX 导出项（沿用查询脱敏结果）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptTboxExportVo {

    @Excel(name = "TBOX SN", width = 24)
    private String sn;

    @Excel(name = "当前VIN", width = 22)
    private String vin;

    @Excel(name = "零件编码", width = 20)
    private String partCode;

    @Excel(name = "设备状态", width = 12)
    private String deviceStatus;

    @Excel(name = "首次登录时间", dateFormat = "yyyy-MM-dd HH:mm:ss", width = 22)
    private LocalDateTime firstLoginTime;

    @Excel(name = "最后登录时间", dateFormat = "yyyy-MM-dd HH:mm:ss", width = 22)
    private LocalDateTime lastLoginTime;

    @Excel(name = "激活时间", dateFormat = "yyyy-MM-dd HH:mm:ss", width = 22)
    private LocalDateTime activateTime;

    @Excel(name = "绑定状态", width = 14)
    private String bindingStatus;

    @Excel(name = "异常标签", width = 28)
    private String anomalyTags;

    @Excel(name = "激活耗时(秒)", width = 14)
    private Long activationDurationSeconds;

    @Excel(name = "离线时长(秒)", width = 14)
    private Long lastLoginAgeSeconds;

    @Excel(name = "零件号", width = 20)
    private String hardwareNo;

    @Excel(name = "软件零件号", width = 20)
    private String softwareNo;

    @Excel(name = "配置字", width = 18)
    private String configWord;

    @Excel(name = "供应商", width = 14)
    private String supplierCode;

    @Excel(name = "硬件版本", width = 14)
    private String hardwareVersion;

    @Excel(name = "软件版本", width = 14)
    private String softwareVersion;

    @Excel(name = "HSM UID", width = 22)
    private String maskedHsmUid;

    @Excel(name = "IMEI", width = 22)
    private String maskedImei;

    @Excel(name = "ICCID", width = 24)
    private String maskedIccids;

    @Excel(name = "绑定版本", width = 12)
    private Long bindingVersion;

    @Excel(name = "创建时间", dateFormat = "yyyy-MM-dd HH:mm:ss", width = 22)
    private LocalDateTime createdAt;

    @Excel(name = "更新时间", dateFormat = "yyyy-MM-dd HH:mm:ss", width = 22)
    private LocalDateTime updatedAt;
}
