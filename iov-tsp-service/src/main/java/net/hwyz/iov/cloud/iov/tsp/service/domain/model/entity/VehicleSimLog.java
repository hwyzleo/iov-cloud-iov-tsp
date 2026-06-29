package net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车辆SIM卡审计日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSimLog {
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
}
