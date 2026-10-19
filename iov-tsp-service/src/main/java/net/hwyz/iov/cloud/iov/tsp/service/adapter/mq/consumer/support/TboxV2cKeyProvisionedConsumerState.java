package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

import lombok.Data;

import java.time.Instant;

/**
 * V2C 聚合成功事件消费者运行状态（TSP-DSN-CR-012 §9）
 * <p>
 * 供 keyprovActivation 专用 HealthIndicator 读取；不保存 deviceSn、requestId、keyId 等敏感信息。
 *
 * @author hwyz_leo
 */
@Data
public class TboxV2cKeyProvisionedConsumerState {

    /**
     * 消费者是否已启动
     */
    private volatile boolean started;

    /**
     * 是否已完成 Topic 订阅
     */
    private volatile boolean subscribed;

    /**
     * 最近一次收到记录的时间
     */
    private volatile Instant lastPollAt;

    /**
     * 最近一次成功处理（含幂等）的时间
     */
    private volatile Instant lastSuccessAt;

    /**
     * 最近一次进入 DLT 的时间
     */
    private volatile Instant lastDltAt;

    /**
     * 累计成功（含幂等）数
     */
    private volatile long successTotal;

    /**
     * 累计 DLT 数
     */
    private volatile long dltTotal;

    /**
     * 累计重试次数
     */
    private volatile long retryTotal;
}
