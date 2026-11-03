package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TspViamInboxRecord;

import java.util.Optional;

/**
 * TSP 消费 VIAM 安全事件幂等 Inbox 仓储接口（IOV-TSP-DSN-CR-016）
 * <p>
 * 仅保存契约摘要与处理状态，不保存敏感 payload 原文；payload 由 Kafka 重投/重放提供。
 * </p>
 *
 * @author hwyz_leo
 */
public interface TspViamInboxRepository {

    /**
     * 根据事件ID查询（幂等键去重）
     *
     * @param eventId 事件ID
     * @return Inbox 记录
     */
    Optional<TspViamInboxRecord> findByEventId(String eventId);

    /**
     * 保存消费记录（同 eventId 唯一键约束防重）
     *
     * @param record Inbox 记录
     */
    void save(TspViamInboxRecord record);

    /**
     * 标记处理完成
     *
     * @param eventId      事件ID
     * @param processedAt  处理完成时间（UTC 墙钟）
     * @return 是否更新成功
     */
    boolean markProcessed(String eventId, java.time.LocalDateTime processedAt);

    /**
     * 标记可重试/对账（资产未导入等），retry_count 递增
     *
     * @param eventId 事件ID
     * @param error   脱敏错误摘要
     */
    void markRetryable(String eventId, String error);

    /**
     * 标记契约冲突（同 eventId 不同内容，不静默当重复成功）
     *
     * @param eventId 事件ID
     * @param error   冲突摘要
     */
    void markConflict(String eventId, String error);

    /**
     * 标记处理失败（重试耗尽；错误可见、可重放，不静默丢弃）
     *
     * @param eventId 事件ID
     * @param error   脱敏错误摘要
     */
    void markFailed(String eventId, String error);
}
