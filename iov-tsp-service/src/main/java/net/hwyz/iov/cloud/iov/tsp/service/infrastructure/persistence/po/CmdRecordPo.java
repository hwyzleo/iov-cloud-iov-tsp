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

import java.util.Date;

/**
 * <p>
 * TBOX指令记录表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-08-30
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_cmd_record")
public class CmdRecordPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 车架号
     */
    @TableField("vin")
    private String vin;

    /**
     * 指令ID
     */
    @TableField("cmd_id")
    private String cmdId;

    /**
     * 指令类型
     */
    @TableField("cmd_type")
    private String cmdType;

    /**
     * 指令参数
     */
    @TableField("cmd_param")
    private String cmdParam;

    /**
     * 消息流向
     */
    @TableField("msg_flow")
    private Integer msgFlow;

    /**
     * 消息重试次数
     */
    @TableField("msg_retry")
    private Integer msgRetry;

    /**
     * 消息时间
     */
    @TableField("msg_time")
    private Date msgTime;

    /**
     * 消息确认时间
     */
    @TableField("msg_ack_time")
    private Date msgAckTime;
}
