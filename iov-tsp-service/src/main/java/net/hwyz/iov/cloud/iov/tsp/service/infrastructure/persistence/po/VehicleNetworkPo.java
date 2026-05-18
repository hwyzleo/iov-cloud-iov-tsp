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
 * 车辆网联信息表 数据对象
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
@TableName("tb_vehicle_network")
public class VehicleNetworkPo extends BasePo {

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
     * 卡1在线状态
     */
    @TableField("iccid1_online")
    private Boolean iccid1Online;

    /**
     * 集成电路卡识别码2
     */
    @TableField("iccid2")
    private String iccid2;

    /**
     * 卡2在线状态
     */
    @TableField("iccid2_online")
    private Boolean iccid2Online;

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
