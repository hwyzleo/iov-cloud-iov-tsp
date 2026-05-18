package net.hwyz.iov.cloud.iov.tsp.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对外服务SIM卡信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimVo {

    /**
     * 集成电路卡识别码
     */
    private String iccid;

    /**
     * 国际移动用户识别号
     */
    private String imsi;

    /**
     * 手机号
     */
    private String msisdn;

}
