package net.hwyz.iov.cloud.iov.tsp.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对外服务车辆中央计算平台
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCcpVo {

    /**
     * 车架号
     */
    private String vin;

    /**
     * 序列号
     */
    private String sn;

}
