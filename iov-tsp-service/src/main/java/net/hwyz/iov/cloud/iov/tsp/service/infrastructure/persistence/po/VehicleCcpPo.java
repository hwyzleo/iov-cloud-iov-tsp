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
import java.time.Instant;

/**
 * <p>
 * 车辆中央计算平台表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-23
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_vehicle_ccp")
public class VehicleCcpPo extends BasePo {

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
     * 序列号
     */
    @TableField("sn")
    private String sn;

    /**
     * VMD binding_id，投影幂等主键
     */
    @TableField("binding_id")
    private Long bindingId;

    /**
     * VMD零件编码
     */
    @TableField("part_code")
    private String partCode;

    /**
     * 车载节点
     */
    @TableField("vehicle_node_code")
    private String vehicleNodeCode;

    /**
     * 设备类别: TBOX/CCP
     */
    @TableField("device_category")
    private String deviceCategory;

    /**
     * 绑定状态: ACTIVE/INACTIVE
     */
    @TableField("bind_state")
    private String bindState;

    /**
     * 绑定版本，映射VMD seq
     */
    @TableField("binding_version")
    private Long bindingVersion;

    /**
     * 最近事件时间
     */
    @TableField("last_event_time")
    private Instant lastEventTime;

    /**
     * 投影来源
     */
    @TableField("source")
    private String source;
}
