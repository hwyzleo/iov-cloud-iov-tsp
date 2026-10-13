package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 设备接入准入指标（TSP-DSN-CR-011 §9）
 * <p>
 * 指标 label 不得包含 uid、VIN、serial、fingerprint、clientId、IP 或 endpoint。
 * CERT_REVOKED 与 DEPENDENCY_UNAVAILABLE 必须分开计数。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class DeviceAdmissionMetrics {

    private static final String METRIC_DECISION = "tsp_device_admission_total";
    private static final String METRIC_LATENCY = "tsp_device_admission_latency_seconds";
    private static final String METRIC_PKI_QUERY = "tsp_pki_revocation_query_total";
    private static final String METRIC_PKI_LATENCY = "tsp_pki_revocation_latency_seconds";
    private static final String METRIC_PKI_REVOKED = "tsp_pki_revoked_total";
    private static final String METRIC_PKI_DEPENDENCY = "tsp_pki_dependency_unavailable_total";

    private final MeterRegistry meterRegistry;

    public DeviceAdmissionMetrics(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    /**
     * 裁决结果计数。
     */
    public void decision(String decision, String reason) {
        counter(METRIC_DECISION, "decision", decision, "reason", reason);
    }

    /**
     * 端到端裁决耗时。
     */
    public void latency(long latencyMs) {
        timer(METRIC_LATENCY, null, null, latencyMs);
    }

    /**
     * 吊销查询结果计数（source=OCSP/CRL/UNKNOWN，outcome=VALID/REVOKED/EXPIRED/UNKNOWN）。
     */
    public void pkiQuery(String source, String outcome) {
        counter(METRIC_PKI_QUERY, "source", source, "outcome", outcome);
    }

    /**
     * 吊销查询耗时。
     */
    public void pkiLatency(String source, long latencyMs) {
        timer(METRIC_PKI_LATENCY, "source", source, latencyMs);
    }

    /**
     * 确认吊销计数（安全事件）。
     */
    public void revoked() {
        counter(METRIC_PKI_REVOKED, null, null, null, null);
    }

    /**
     * 依赖不可用计数（category=固定分类）。
     */
    public void dependencyUnavailable(String category) {
        counter(METRIC_PKI_DEPENDENCY, "category", category, null, null);
    }

    private void counter(String name, String tagKeyA, String tagValueA, String tagKeyB, String tagValueB) {
        if (meterRegistry == null) {
            return;
        }
        Counter.Builder builder = Counter.builder(name);
        if (tagKeyA != null) {
            builder.tag(tagKeyA, sanitize(tagValueA));
        }
        if (tagKeyB != null) {
            builder.tag(tagKeyB, sanitize(tagValueB));
        }
        builder.register(meterRegistry).increment();
    }

    private void timer(String name, String tagKey, String tagValue, long latencyMs) {
        if (meterRegistry == null) {
            return;
        }
        Timer.Builder builder = Timer.builder(name);
        if (tagKey != null) {
            builder.tag(tagKey, sanitize(tagValue));
        }
        builder.register(meterRegistry).record(Duration.ofMillis(Math.max(latencyMs, 0)));
    }

    private static String sanitize(String value) {
        return value == null ? "UNKNOWN" : value.replaceAll("[^A-Za-z0-9_]", "_");
    }
}
