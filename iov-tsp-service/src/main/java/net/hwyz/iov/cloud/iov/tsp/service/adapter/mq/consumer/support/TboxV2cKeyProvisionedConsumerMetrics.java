package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.TboxV2cActivationOutcome;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * V2C 聚合成功事件消费者指标（TSP-DSN-CR-012 §9）
 * <p>
 * 指标：{@code tsp_keyprov_event_total{outcome,reason}}、{@code tsp_keyprov_activation_latency}、
 * {@code tsp_keyprov_event_retry_total{reason}}、{@code tsp_keyprov_event_dlt_total{reason}}。
 * label 禁止包含 deviceSn、requestId、keyId、VIN 或密码学摘要。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class TboxV2cKeyProvisionedConsumerMetrics {

    static final String EVENT_TOTAL = "tsp_keyprov_event_total";
    static final String ACTIVATION_LATENCY = "tsp_keyprov_activation_latency";
    static final String RETRY_TOTAL = "tsp_keyprov_event_retry_total";
    static final String DLT_TOTAL = "tsp_keyprov_event_dlt_total";

    private final TboxV2cKeyProvisionedConsumerState state;
    private final MeterRegistry meterRegistry;

    public TboxV2cKeyProvisionedConsumerMetrics(TboxV2cKeyProvisionedConsumerState state,
                                                ObjectProvider<MeterRegistry> meterRegistry) {
        this.state = state;
        this.meterRegistry = meterRegistry.getIfAvailable();
    }

    /**
     * 记录一次消费终态（成功/幂等/永久拒绝）。
     */
    public void processed(TboxV2cActivationOutcome outcome) {
        if (outcome.isSuccess()) {
            state.setLastSuccessAt(Instant.now());
            state.setSuccessTotal(state.getSuccessTotal() + 1);
        }
        if (meterRegistry != null) {
            Counter.builder(EVENT_TOTAL)
                    .tag("outcome", outcome.name())
                    .tag("reason", outcome.isSuccess() ? "SUCCESS" : "REJECTED")
                    .register(meterRegistry)
                    .increment();
        }
    }

    /**
     * 记录一次瞬时/可重试失败。
     */
    public void retry(String reason) {
        state.setRetryTotal(state.getRetryTotal() + 1);
        if (meterRegistry != null) {
            Counter.builder(RETRY_TOTAL)
                    .tag("reason", sanitize(reason))
                    .register(meterRegistry)
                    .increment();
        }
    }

    /**
     * 记录一次进入 DLT。
     */
    public void dlt(String reason) {
        state.setLastDltAt(Instant.now());
        state.setDltTotal(state.getDltTotal() + 1);
        if (meterRegistry != null) {
            Counter.builder(DLT_TOTAL)
                    .tag("reason", sanitize(reason))
                    .register(meterRegistry)
                    .increment();
        }
    }

    /**
     * 记录激活时延（事件 occurredAt → 本地激活完成）。
     */
    public void activationLatency(Instant occurredAt) {
        if (occurredAt == null || meterRegistry == null) {
            return;
        }
        Duration latency = Duration.between(occurredAt, Instant.now());
        Timer.builder(ACTIVATION_LATENCY)
                .register(meterRegistry)
                .record(latency.isNegative() ? Duration.ZERO : latency);
    }

    private static String sanitize(String reason) {
        return reason == null ? "UNKNOWN" : reason.replaceAll("[^A-Z0-9_]", "_");
    }
}
