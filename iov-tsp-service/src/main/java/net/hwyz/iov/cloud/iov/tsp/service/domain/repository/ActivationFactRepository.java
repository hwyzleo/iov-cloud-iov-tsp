package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationFact;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 首次激活候选事实 + 投递 Outbox 仓储。
 *
 * @author hwyz_leo
 */
public interface ActivationFactRepository {

    /**
     * 按事件ID查询
     *
     * @param eventId 事件ID
     * @return 候选事实
     */
    Optional<ActivationFact> findByEventId(String eventId);

    /**
     * 按业务键查询（保存唯一候选事实）
     *
     * @param vin        车辆 VIN
     * @param operationId 绑定操作ID
     * @return 候选事实
     */
    Optional<ActivationFact> findByBusinessKey(String vin, String operationId);

    /**
     * 新增候选事实（与绑定事务同事务）
     *
     * @param fact 候选事实
     */
    void insert(ActivationFact fact);

    /**
     * 查询待投递事实
     *
     * @param limit 批量上限
     * @return 待投递事实
     */
    List<ActivationFact> findPending(int limit);

    /**
     * 标记投递成功
     *
     * @param eventId 事件ID
     * @param now     投递成功时刻
     * @return 更新行数
     */
    int markPublished(String eventId, Instant now);

    /**
     * 标记投递失败（保留可重试结果）
     *
     * @param eventId 事件ID
     * @param error   脱敏错误摘要
     * @param status  目标状态（未达重试上限保持 PENDING，达到上限转 FAILED）
     * @param now     当前时刻
     * @return 更新行数
     */
    int markFailed(String eventId, String error, String status, Instant now);
}
