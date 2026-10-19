package net.hwyz.iov.cloud.iov.tsp.api.vo.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * TBOX V2C 上下行业务密钥件内导入完整成功事件（VMD -> TSP）
 * <p>
 * VMD 经 Outbox→Relay 发布到 Topic vmd.tbox-v2c-key-provisioned（Kafka Key=deviceSn）；
 * 仅供 TSP 收口设备注册激活（ACTIVATING→ACTIVE）。payload 仅含非敏感元数据，
 * 不含 VIN、HSM UID、证书、kmsKeyRef、Wrapped Key 或任何明文材料。
 * <p>
 * TSP-DSN-CR-012：自本 CR 起 {@code result=SUCCEEDED} 的语义由 VMD CR-062 冻结为
 * “双用途 Import Attestation 真实验签 + 证书绑定 + VMD 同一事务原子 ACTIVE 已提交”。
 * 事件契约版本 {@link #contractVersion} 为可选字段：缺省视为 {@link #CONTRACT_VERSION_V1}，
 * 未知非空版本一律 fail-closed；wire 格式或成功语义变化必须发布新版本，不得重新解释 v1。
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
     * 当前事件契约版本（VMD CR-062 成功语义）
     */
    public static final String CONTRACT_VERSION_V1 = "1";

    /**
     * v1 的兼容别名（生产侧历史写法）
     */
    public static final String CONTRACT_VERSION_V1_ALIAS = "v1";

    /**
     * 受支持的契约版本集合；未知非空版本 fail-closed
     */
    public static final Set<String> SUPPORTED_CONTRACT_VERSIONS =
            Set.of(CONTRACT_VERSION_V1, CONTRACT_VERSION_V1_ALIAS);

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
     * 事件契约版本（可选，缺省 v1）
     */
    private String contractVersion;

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
