package net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tbox {

    private Long id;

    private String sn;

    private String no;

    private String configWord;

    private String supplierCode;

    private String hardwareVer;

    private String softwareVer;

    private String hardwareNo;

    private String softwareNo;

    private String hsm;

    private Integer deviceStatus;

    private LocalDateTime activateTime;

    private LocalDateTime firstLoginTime;

    private LocalDateTime lastLoginTime;

    /**
     * VIAM 投影同步状态: UNKNOWN/SYNCED/PARTIAL/STALE；NULL=从未同步
     */
    private String viamSyncStatus;

    /**
     * VIAM 投影最后成功对账/快照应用时间（≠事实时间）
     */
    private LocalDateTime viamLastReconciledAt;

    /**
     * VIAM 安全状态版本（预留；当前 VIAM 快照 API 未提供，NULL=未知）
     */
    private Long viamSecurityVersion;

    /**
     * TSP 运营退役时间（独立表达，与安全 BLOCKED/RETIRED 分离；NULL=未运营退役）
     */
    private LocalDateTime operationalRetiredAt;

    private String iccid1;

    private String iccid2;

    private String imei;

    private String description;

    private String createBy;

    private String modifyBy;

}