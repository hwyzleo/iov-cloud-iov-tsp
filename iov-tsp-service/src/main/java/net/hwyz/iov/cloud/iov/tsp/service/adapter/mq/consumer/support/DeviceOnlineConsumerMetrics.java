package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.DeviceOnlineOutcome;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * DEVICE_ONLINE 消费者指标（TSP-DSN-CR-010 §8）
 * <p>
 * 指标 label 不得包含 hsmUid、VIN、sessionId、certSerial 或 tboxSn。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class DeviceOnlineConsumerMetrics {

    private final DeviceOnlineConsumerState state;
    private final MeterRegistry meterRegistry;

    public DeviceOnlineConsumerMetrics(DeviceOnlineConsumerState state, ObjectProvider<MeterRegistry> meterRegistry) {
        this.state = state;
        this.meterRegistry = meterRegistry.getIfAvailable();
    }

    public void success(DeviceOnlineOutcome outcome) {
        state.setLastSuccessAt(java.time.Instant.now());
        state.setSuccessTotal(state.getSuccessTotal() + 1);
        if (meterRegistry != null) {
            Counter.builder("tsp_device_online_consume_total")
                    .tag("result", outcome.name())
                    .register(meterRegistry)
                    .increment();
        }
    }

    public void retry() {
        state.setRetryTotal(state.getRetryTotal() + 1);
        if (meterRegistry != null) {
            Counter.builder("tsp_device_online_retry_total").register(meterRegistry).increment();
        }
    }

    public void rejected(String reason) {
        state.setRejectedTotal(state.getRejectedTotal() + 1);
        state.setLastDltAt(java.time.Instant.now());
        if (meterRegistry != null) {
            Counter.builder("tsp_device_online_rejected_total")
                    .tag("reason", sanitize(reason))
                    .register(meterRegistry)
                    .increment();
        }
    }

    private static String sanitize(String reason) {
        return reason == null ? "UNKNOWN" : reason.replaceAll("[^A-Z0-9_]", "_");
    }
}
