package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TBOX 运营查询视图
 * <p>
 * TSP-DSN-CR-009：由 tb_tbox 与 tb_vehicle_tbox（TBOX grain 聚合）组成的只读查询视图。
 * bindCount 为当前 ACTIVE 绑定数量；仅当 bindCount==1 时 vin/partCode/vehicleNodeCode/bindingId/bindingVersion/bindingUpdatedAt 有效。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TboxManagementView {

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

    private LocalDateTime firstLoginTime;

    private LocalDateTime lastLoginTime;

    private LocalDateTime activateTime;

    private String iccid1;

    private String iccid2;

    private String imei;

    private LocalDateTime createTime;

    private LocalDateTime modifyTime;

    /**
     * 当前 ACTIVE TBOX 绑定数量
     */
    private Integer bindCount;

    private String vin;

    private String partCode;

    private String vehicleNodeCode;

    private Long bindingId;

    private Long bindingVersion;

    private LocalDateTime bindingUpdatedAt;
}
