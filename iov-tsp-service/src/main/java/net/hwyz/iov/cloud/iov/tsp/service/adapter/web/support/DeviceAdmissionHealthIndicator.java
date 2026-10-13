package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.support;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.DeviceAdmissionProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

/**
 * 设备接入准入专用健康检查（TSP-DSN-CR-011 §8）
 * <p>
 * 发布门禁 {@code tsp.device-admission.pki-revocation.enabled} 关闭时准入能力为 NOT_READY；
 * 门禁关闭不改变准入请求的 fail-closed 行为。该健康组不拖垮查询类能力的全局 readiness。
 * 不输出 uid、证书等敏感信息。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class DeviceAdmissionHealthIndicator implements HealthIndicator {

    private final DeviceAdmissionProperties properties;

    @Override
    public Health health() {
        boolean enabled = properties.getPkiRevocation().isEnabled();
        if (!enabled) {
            return Health.status(Status.OUT_OF_SERVICE)
                    .withDetail("pkiRevocationEnabled", false)
                    .withDetail("reason", "device admission PKI revocation gate disabled")
                    .build();
        }
        return Health.up()
                .withDetail("pkiRevocationEnabled", true)
                .build();
    }
}
