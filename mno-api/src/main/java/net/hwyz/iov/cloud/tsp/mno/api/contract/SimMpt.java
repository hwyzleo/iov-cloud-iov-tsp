package net.hwyz.iov.cloud.tsp.mno.api.contract;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.web.domain.BaseRequest;

import java.util.Date;

/**
 * 管理后台SIM卡
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SimMpt extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

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

    /**
     * 运营商编码
     */
    private String mnoCode;

    /**
     * SIM卡状态：1-测试，2-库存，3-激活
     */
    private Integer simState;

    /**
     * 短信能力
     */
    private Boolean smsAbility;

    /**
     * 数据能力
     */
    private Boolean dataAbility;

    /**
     * 语音能力
     */
    private Boolean voiceAbility;

    /**
     * 创建时间
     */
    private Date createTime;

}
