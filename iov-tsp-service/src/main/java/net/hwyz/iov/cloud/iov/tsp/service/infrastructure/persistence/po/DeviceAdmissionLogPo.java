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
 * 设备接入鉴权审计日志表 数据对象
 * </p>
 * <p>
 * TSP-DSN-CR-011 §9：新记录只写不可逆摘要（uid_digest/cert_serial_digest/source_ip_digest），
 * 旧列 hsm 标记 deprecated，停止写入明文。
 *
 * @author hwyz_leo
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
     * 硬件安全模块标识（deprecated：停止写入明文，仅历史数据保留）
     */
    @TableField("hsm")
    @Deprecated
    private String hsm;

    /**
     * 裁决结果（ALLOW/DENY）
     */
    @TableField("admission_result")
    private String admissionResult;

    /**
     * 裁决原因（语义枚举名）
     */
    @TableField("reason")
    private String reason;

    /**
     * uid 检查结果（PASS/FAIL，deprecated 语义）
     */
    @TableField("hsm_check_result")
    private String hsmCheckResult;

    /**
     * 证书状态（VALID/REVOKED/EXPIRED/UNKNOWN，deprecated 列名）
     */
    @TableField("pki_check_result")
    private String pkiCheckResult;

    /**
     * 设备状态检查结果（PASS/FAIL）
     */
    @TableField("device_status_check_result")
    private String deviceStatusCheckResult;

    /**
     * uid 不可逆摘要
     */
    @TableField("uid_digest")
    private String uidDigest;

    /**
     * 证书序列号不可逆摘要
     */
    @TableField("cert_serial_digest")
    private String certSerialDigest;

    /**
     * 来源 IP 不可逆摘要
     */
    @TableField("source_ip_digest")
    private String sourceIpDigest;

    /**
     * 证书检查完成时刻
     */
    @TableField("checked_at")
    private Instant checkedAt;

    /**
     * 吊销状态来源（OCSP/CRL）
     */
    @TableField("revocation_source")
    private String revocationSource;

    /**
     * 依赖处置结果
     */
    @TableField("dependency_outcome")
    private String dependencyOutcome;

    /**
     * 调用方标识
     */
    @TableField("caller")
    private String caller;

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
