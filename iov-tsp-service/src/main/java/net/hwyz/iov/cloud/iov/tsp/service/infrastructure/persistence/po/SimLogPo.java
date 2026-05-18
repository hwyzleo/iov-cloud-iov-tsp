package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * SIM卡信息变更日志表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-13
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_sim_log")
public class SimLogPo extends BasePo {

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
