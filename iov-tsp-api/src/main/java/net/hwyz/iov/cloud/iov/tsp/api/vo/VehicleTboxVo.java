package net.hwyz.iov.cloud.iov.tsp.api.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对外服务车辆车联终端
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTboxVo {

    /**
     * 车架号
     */
    @NotBlank(message = "车架号不能为空")
    private String vin;

    /**
     * 序列号
     */
    @NotBlank(message = "序列号不能为空")
    private String sn;

}
