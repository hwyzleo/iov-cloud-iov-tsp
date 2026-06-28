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

    /**
     * 绑定ID
     */
    private Long bindingId;

    /**
     * 零件编码
     */
    private String partCode;

    /**
     * 车辆节点编码
     */
    private String vehicleNodeCode;

    /**
     * 设备类别
     */
    private String deviceCategory;

    /**
     * 绑定状态
     */
    private String bindState;

    /**
     * 绑定版本
     */
    private Long bindingVersion;

    /**
     * 最后事件时间
     */
    private String lastEventTime;

    /**
     * 数据来源
     */
    private String source;

}
