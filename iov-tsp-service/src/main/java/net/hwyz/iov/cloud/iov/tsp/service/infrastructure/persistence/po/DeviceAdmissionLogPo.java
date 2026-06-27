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
 * 设备接入鉴权审计日志表 数据对象
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
@TableName("tb_device_admission_log")
public class DeviceAdmissionLogPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 车辆识别码
     */
    @TableField("vin")
    private String vin;

    /**
     * 硬件安全模块标识
     */
    @TableField("hsm")
    private String hsm;

    /**
     * 鉴权结果
     */
    @TableField("admission_result")
    private String admissionResult;

    /**
     * 鉴权原因
     */
    @TableField("reason")
    private String reason;

    /**
     * HSM检查结果
     */
    @TableField("hsm_check_result")
    private String hsmCheckResult;

    /**
     * PKI检查结果
     */
    @TableField("pki_check_result")
    private String pkiCheckResult;

    /**
     * 设备状态检查结果
     */
    @TableField("device_status_check_result")
    private String deviceStatusCheckResult;

    /**
     * 请求上下文
     */
    @TableField("request_context")
    private String requestContext;

    /**
     * 响应时间(毫秒)
     */
    @TableField("response_time_ms")
    private Integer responseTimeMs;
}
