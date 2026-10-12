package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * TBOX 注册激活事件消费记录（轻量 Inbox）
 * <p>
 * TSP-DSN-CR-008：保存消费幂等事实，不保存 keyId 列表或密钥材料。
 *
 * @param eventId        事件唯一标识
 * @param requestId      TBOX 稳定请求ID（DEVICE_ONLINE 为 null）
 * @param deviceSn       设备序列号/规范身份
 * @param eventType      事件类型
 * @param occurredAt     事件发生时间
 * @param processedAt    处理时间
 * @param result         处理结果
 * @param contractDigest 事件契约不可逆摘要（不含 keyId），可为空
 * @author hwyz_leo
 */
public record TboxRegistrationEventRecord(
        String eventId,
        String requestId,
        String deviceSn,
        String eventType,
        Instant occurredAt,
        LocalDateTime processedAt,
        String result,
        String contractDigest) {
}
