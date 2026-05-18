package net.hwyz.iov.cloud.iov.tsp.api.vo.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.RemoteControlType;

import java.util.Map;

/**
 * 远控请求
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoteControlRequest {

    /**
     * 车架号
     */
    @NotEmpty(message = "车架号不允许为空")
    private String vin;
    /**
     * 远控类型
     */
    @NotNull(message = "远控类型不允许为空")
    private RemoteControlType type;
    /**
     * 远控参数
     */
    private Map<String, Object> params;
}
