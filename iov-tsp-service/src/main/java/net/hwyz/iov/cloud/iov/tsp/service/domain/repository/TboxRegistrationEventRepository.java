package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxRegistrationEventRecord;

import java.util.Optional;

/**
 * TBOX 注册激活事件消费记录仓储接口（轻量 Inbox）
 *
 * @author hwyz_leo
 */
public interface TboxRegistrationEventRepository {

    /**
     * 根据事件ID查询消费记录
     *
     * @param eventId 事件ID
     * @return 消费记录
     */
    Optional<TboxRegistrationEventRecord> findByEventId(String eventId);

    /**
     * 根据事件类型与请求ID查询消费记录
     *
     * @param eventType 事件类型
     * @param requestId 请求ID
     * @return 消费记录
     */
    Optional<TboxRegistrationEventRecord> findByEventTypeAndRequestId(String eventType, String requestId);

    /**
     * 保存消费记录
     *
     * @param record 消费记录
     */
    void save(TboxRegistrationEventRecord record);
}
