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
 * 车联终端信息表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-14
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_tbox")
public class TboxPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 序列号
     */
    @TableField("sn")
    private String sn;

    /**
     * 零件编号
     */
    @TableField("no")
    private String no;

    /**
     * 配置字
     */
    @TableField("config_word")
    private String configWord;

    /**
     * 供应商编码
     */
    @TableField("supplier_code")
    private String supplierCode;

    /**
     * 硬件版本号
     */
    @TableField("hardware_ver")
    private String hardwareVer;

    /**
     * 软件版本号
     */
    @TableField("software_ver")
    private String softwareVer;

    /**
     * 硬件零件号
     */
    @TableField("hardware_no")
    private String hardwareNo;

    /**
     * 软件零件号
     */
    @TableField("software_no")
    private String softwareNo;

    /**
     * 硬件安全模块
     */
    @TableField("hsm")
    private String hsm;

    /**
     * 设备状态: 1-活跃, 2-未激活, 3-暂停, 4-停用
     */
    @TableField("device_status")
    private Integer deviceStatus;

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
     * 国际移动设备识别码
     */
    @TableField("imei")
    private String imei;
}
