package net.hwyz.iov.cloud.iov.tsp.api.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对外服务车辆网联信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleNetworkVo {

    /**
     * 车架号
     */
    @NotBlank(message = "车架号不能为空")
    private String vin;

    /**
     * 集成电路卡识别码1
     */
    @NotBlank(message = "集成电路卡识别码1不能为空")
    private String iccid1;

    /**
     * 集成电路卡识别码2
     */
    private String iccid2;

}
