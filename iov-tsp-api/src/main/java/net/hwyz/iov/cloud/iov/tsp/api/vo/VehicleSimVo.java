package net.hwyz.iov.cloud.iov.tsp.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 车辆SIM卡VO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSimVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 卡槽序号
     */
    private Integer slotNo;

    /**
     * SIM卡号
     */
    private String iccid;

    /**
     * SIM卡生命周期状态
     */
    private Integer simStatus;

    /**
     * 实名态
     */
    private Integer realnameStatus;

    /**
     * 套餐编码
     */
    private String packageCode;

}
