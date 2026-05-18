package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

/**
 * <p>
 * SIM卡信息表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-12
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_sim")
public class SimPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 集成电路卡识别码
     */
    @TableField("iccid")
    private String iccid;

    /**
     * 国际移动用户识别号
     */
    @TableField("imsi")
    private String imsi;

    /**
     * 手机号
     */
    @TableField("msisdn")
    private String msisdn;

    /**
     * 运营商编码
     */
    @TableField("mno_code")
    private String mnoCode;

    /**
     * SIM卡状态：1-测试，2-库存，3-激活
     */
    @TableField("sim_state")
    private Integer simState;

    /**
     * 短信能力
     */
    @TableField("sms_ability")
    private Boolean smsAbility;

    /**
     * 数据能力
     */
    @TableField("data_ability")
    private Boolean dataAbility;

    /**
     * 语音能力
     */
    @TableField("voice_ability")
    private Boolean voiceAbility;
}
