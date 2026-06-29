package net.hwyz.iov.cloud.iov.tsp.api.vo.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * CCS SIM状态变更事件
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CcsSimStatusChangedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 车架号
     */
    private String vin;

    /**
     * SIM卡号
     */
    private String iccid;

    /**
     * SIM卡状态
     */
    private Integer simStatus;

    /**
     * 实名状态
     */
    private Integer realnameStatus;

    /**
     * 事件版本号
     */
    private Long version;

}
