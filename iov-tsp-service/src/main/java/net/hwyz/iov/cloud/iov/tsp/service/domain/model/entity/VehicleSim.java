package net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车辆SIM卡领域实体
 * 按卡一行，取代原 VehicleNetwork
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSim {
    private Long id;
    private String vin;
    private Integer slotNo;
    private String iccid;
    private Integer simStatus;
    private Integer realnameStatus;
    private String packageCode;
    private Long sourceVersion;
    private String description;
    private String createBy;
    private String modifyBy;

    /**
     * 初始化默认状态
     */
    public void initDefaultState() {
        this.simStatus = null;
        this.realnameStatus = null;
        this.sourceVersion = null;
    }

    /**
     * 应用SIM状态投影
     * 仅当 version 更新时才应用
     * 
     * @param simStatus SIM卡状态
     * @param realnameStatus 实名状态
     * @param version 事件版本号
     * @return 是否应用成功
     */
    public boolean applySimStatusProjection(Integer simStatus, Integer realnameStatus, Long version) {
        if (version == null) {
            return false;
        }
        if (this.sourceVersion != null && version <= this.sourceVersion) {
            return false;
        }
        this.simStatus = simStatus;
        this.realnameStatus = realnameStatus;
        this.sourceVersion = version;
        return true;
    }
}
