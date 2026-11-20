package net.hwyz.iov.cloud.iov.tsp.service.domain.port;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationFact;

/**
 * 首次激活候选事实投递端口（Outbox 排空）。
 *
 * @author hwyz_leo
 */
public interface ActivationFactPublisher {

    /**
     * 投递单条候选事实
     *
     * @param fact 候选事实
     * @throws Exception 投递失败（调用方保留可重试结果，不回滚已确认关系）
     */
    void publish(ActivationFact fact) throws Exception;
}
