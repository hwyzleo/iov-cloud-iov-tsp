package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

import lombok.Data;

import java.time.Instant;

/**
 * DEVICE_ONLINE 消费者运行状态
 * <p>
 * TSP-DSN-CR-010 §8：供注册激活专用 HealthIndicator 读取，不直接对外暴露身份信息。
 *
 * @author hwyz_leo
 */
@Data
public class DeviceOnlineConsumerState {

    /**
     * 消费者是否已启动订阅
     */
    private volatile boolean started;

    /**
     * 是否已完成 topic 订阅（assign 之前）
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
     * 累计拒绝（DLT）数
     */
    private volatile long rejectedTotal;

    /**
     * 累计重试次数
     */
    private volatile long retryTotal;
}
