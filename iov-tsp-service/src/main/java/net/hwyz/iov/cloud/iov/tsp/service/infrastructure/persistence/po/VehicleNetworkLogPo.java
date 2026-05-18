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
 * 车辆网联信息变更日志表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-19
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_vehicle_network_log")
public class VehicleNetworkLogPo extends BasePo {

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
     * 集成电路卡识别码1
     */
    @TableField("iccid1")
    private String iccid1;

    /**
     * 集成电路卡识别码2
     */
    @TableField("iccid2")
    private String iccid2;

    /**
     * 网联套餐编码
     */
    @TableField("package_code")
    private String packageCode;

    /**
     * 绑定状态：0-已解绑，1-已绑定
     */
    @TableField("binding")
    private Boolean binding;

    /**
     * 实名认证状态：0-未认证，1-已认证
     */
    @TableField("auth")
    private Boolean auth;
}
