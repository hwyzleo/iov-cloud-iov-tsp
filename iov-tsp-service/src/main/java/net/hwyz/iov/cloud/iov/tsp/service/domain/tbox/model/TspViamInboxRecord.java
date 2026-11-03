package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * TSP 消费 VIAM 安全事件幂等 Inbox 记录（IOV-TSP-DSN-CR-016）
 * <p>
 * 仅保存契约摘要与处理状态，<b>不保存敏感 payload 原文</b>（HSM UID/VIN/sessionId/certSerial
 * 不落库）；payload 由 Kafka 重投/重放提供，重试不依赖 Inbox 原文。事件表唯一写入者为
 * VIAM 安全投影处理链。
 *
 * @param eventId         VIAM 事件唯一ID（幂等键）
 * @param topic           来源 Kafka Topic
 * @param eventType       事件类型（官方契约稳定值）
 * @param eventKey        Kafka Key（principalId 或 deviceSn）
 * @param principalId     VIAM 安全主体ID
 * @param hsmUid          归一化 HSM UID（资产关联键）
 * @param deviceSn        设备序列号
 * @param contractDigest  payload 不可逆摘要（不含敏感字段）
 * @param status          PENDING/PROCESSED/RETRYABLE/CONFLICT/FAILED
 * @param retryCount      重试次数
 * @param error           最近处理错误摘要（脱敏，不含敏感字段）
 * @param receivedAt      接收时间
 * @param processedAt     处理完成时间
 * @author hwyz_leo
 */
public record TspViamInboxRecord(
        String eventId,
        String topic,
        String eventType,
        String eventKey,
        String principalId,
        String hsmUid,
        String deviceSn,
        String contractDigest,
        String status,
        int retryCount,
        String error,
        Instant receivedAt,
        LocalDateTime processedAt) {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSED = "PROCESSED";
    public static final String STATUS_RETRYABLE = "RETRYABLE";
    public static final String STATUS_CONFLICT = "CONFLICT";
    public static final String STATUS_FAILED = "FAILED";
}
