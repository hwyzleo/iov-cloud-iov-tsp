package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * <p>
 * TSP 消费 VIAM 安全事件幂等 Inbox 数据对象（IOV-TSP-DSN-CR-016）
 * </p>
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TspViamInboxPo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * VIAM 事件唯一ID（幂等键）
     */
    private String eventId;

    /**
     * 来源 Kafka Topic
     */
    private String topic;

    /**
     * 事件类型（官方契约稳定值）
     */
    private String eventType;

    /**
     * Kafka Key（principalId 或 deviceSn）
     */
    private String eventKey;

    /**
     * VIAM 安全主体ID
     */
    private String principalId;

    /**
     * 归一化 HSM UID（资产关联键）
     */
    private String hsmUid;

    /**
     * 设备序列号
     */
    private String deviceSn;

    /**
     * payload 不可逆摘要（不存敏感原文）
     */
    private String contractDigest;

    /**
     * 处理状态: PENDING/PROCESSED/RETRYABLE/CONFLICT/FAILED
     */
    private String status;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最近处理错误摘要（脱敏）
     */
    private String error;

    /**
     * 接收时间
     */
    private LocalDateTime receivedAt;

    /**
     * 处理完成时间
     */
    private LocalDateTime processedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime modifyTime;
}
