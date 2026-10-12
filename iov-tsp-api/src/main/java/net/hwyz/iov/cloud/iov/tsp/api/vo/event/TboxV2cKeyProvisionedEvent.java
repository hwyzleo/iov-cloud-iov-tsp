package net.hwyz.iov.cloud.iov.tsp.api.vo.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * TBOX V2C 上下行业务密钥件内导入完整成功事件（VMD -> TSP，CR-058 §6.2）
 * <p>
 * VMD 经 Outbox→Relay 发布到 Topic vmd.tbox-v2c-key-provisioned（Kafka Key=deviceSn）；
 * 仅供 TSP 收口设备注册激活（ACTIVATING→ACTIVE）。payload 仅含非敏感元数据，
 * 不含 VIN、HSM UID、证书、kmsKeyRef、Wrapped Key 或任何明文材料。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TboxV2cKeyProvisionedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 业务域，首期固定 V2C_COMM
     */
    public static final String BUSINESS_DOMAIN_V2C_COMM = "V2C_COMM";

    /**
     * 上行 AEAD 用途
     */
    public static final String PURPOSE_UPLINK_AEAD = "UPLINK_AEAD";

    /**
     * 下行 AEAD 用途
     */
    public static final String PURPOSE_DOWNLINK_AEAD = "DOWNLINK_AEAD";

    /**
     * 聚合结果，仅 SUCCEEDED 才驱动激活
     */
    public static final String RESULT_SUCCEEDED = "SUCCEEDED";

    /**
     * 事件唯一标识
     */
    private String eventId;

    /**
     * TBOX 稳定请求ID
     */
    private String requestId;

    /**
     * TBOX 序列号（同时为 Kafka Key）
     */
    private String deviceSn;

    /**
     * 业务域
     */
    private String businessDomain;

    /**
     * 完整用途密钥元数据（不含密钥材料）
     */
    private List<Key> keys;

    /**
     * 聚合结果
     */
    private String result;

    /**
     * 事件发生时间
     */
    private Instant occurredAt;

    /**
     * 单用途密钥元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 用途代码
         */
        private String purpose;

        /**
         * VMD 业务目录 ID（TSP 不持久化）
         */
        private String keyId;

        /**
         * VMD 业务版本
         */
        private Long businessKeyVersion;
    }
}
