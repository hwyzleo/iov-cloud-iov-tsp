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
 * 车辆SIM卡日志表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-06-29
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_vehicle_sim_log")
public class VehicleSimLogPo extends BasePo {

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
     * 卡槽编号
     */
    @TableField("slot_no")
    private Integer slotNo;

    /**
     * 集成电路卡识别码
     */
    @TableField("iccid")
    private String iccid;

    /**
     * SIM卡状态
     */
    @TableField("sim_status")
    private Integer simStatus;

    /**
     * 实名认证状态
     */
    @TableField("realname_status")
    private Integer realnameStatus;

    /**
     * 套餐编码
     */
    @TableField("package_code")
    private String packageCode;

    /**
     * 数据源版本
     */
    @TableField("source_version")
    private Long sourceVersion;
}
