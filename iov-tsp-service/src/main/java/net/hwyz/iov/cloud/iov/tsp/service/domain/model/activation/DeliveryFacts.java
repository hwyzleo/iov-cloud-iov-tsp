package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

import java.time.Instant;
import java.util.List;

/**
 * 归一化后的最小交付事实（不含姓名、手机号、证件或全订单快照）。
 *
 * @author hwyz_leo
 */
public record DeliveryFacts(DeliveryFactsQuality quality,
                            boolean delivered,
                            List<String> matchedRoles,
                            String orderNo,
                            String factRef,
                            Instant checkedAt) {

    public boolean rolesPresent() {
        return matchedRoles != null && !matchedRoles.isEmpty();
    }
}
