package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.TboxV2cActivationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * V2C 消费指标测试（TSP-DSN-CR-012 §9）
 * <p>
 * 指标名称与 label 必须符合契约，且不得包含 deviceSn/requestId/keyId/VIN/摘要。
 */
class TboxV2cKeyProvisionedConsumerMetricsTest {

    private SimpleMeterRegistry registry;
    private TboxV2cKeyProvisionedConsumerMetrics metrics;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        registry = new SimpleMeterRegistry();
        ObjectProvider<io.micrometer.core.instrument.MeterRegistry> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(registry);
        metrics = new TboxV2cKeyProvisionedConsumerMetrics(new TboxV2cKeyProvisionedConsumerState(), provider);
    }

    @Test
    void processed_ShouldRecordEventOutcome() {
        metrics.processed(TboxV2cActivationOutcome.ACTIVATED);

        Counter counter = registry.get(TboxV2cKeyProvisionedConsumerMetrics.EVENT_TOTAL)
                .tag("outcome", "ACTIVATED")
                .tag("reason", "SUCCESS")
                .counter();
        assertEquals(1.0, counter.count());
    }

    @Test
    void retryAndDlt_ShouldRecordReasonOnlyLabels() {
        metrics.retry("RETRY_OUT_OF_ORDER");
        metrics.dlt("PERMANENT_DEVICE_STATE");

        assertEquals(1.0, registry.get(TboxV2cKeyProvisionedConsumerMetrics.RETRY_TOTAL)
                .tag("reason", "RETRY_OUT_OF_ORDER").counter().count());
        assertEquals(1.0, registry.get(TboxV2cKeyProvisionedConsumerMetrics.DLT_TOTAL)
                .tag("reason", "PERMANENT_DEVICE_STATE").counter().count());
    }

    @Test
    void activationLatency_ShouldRecordTimer() {
        metrics.activationLatency(Instant.now().minusSeconds(2));

        assertEquals(1L, registry.get(TboxV2cKeyProvisionedConsumerMetrics.ACTIVATION_LATENCY)
                .timer().count());
    }

    @Test
    void allMeters_ShouldOnlyUseAllowedTagKeys() {
        metrics.processed(TboxV2cActivationOutcome.ACTIVATED);
        metrics.retry("RETRY_TRANSIENT");
        metrics.dlt("PERMANENT_DEVICE_STATE");
        metrics.activationLatency(Instant.now());

        Set<String> allowed = Set.of("outcome", "reason");
        assertTrue(registry.getMeters().stream()
                .flatMap(meter -> meter.getId().getTags().stream())
                .allMatch(tag -> allowed.contains(tag.getKey())));
    }
}
