package net.hwyz.iov.cloud.iov.tsp.api.external.fallback;

import net.hwyz.iov.cloud.iov.tsp.api.external.CiamBindingRevocationClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * CIAM 绑定撤销契约降级（IOV-TSP-DSN-CR-021 D021-07）。
 * <p>
 * 依赖不可用时必须显式失败，由 TSP 台账记录 PENDING/FAILED 并按原 operationId 重试；
 * 不允许伪造 {@code SUCCEEDED} 或空结果。
 *
 * @author hwyz_leo
 */
@Component
public class CiamBindingRevocationClientFallbackFactory implements FallbackFactory<CiamBindingRevocationClient> {

    @Override
    public CiamBindingRevocationClient create(Throwable cause) {
        return request -> {
            throw new IllegalStateException("SEC-CIAM 绑定撤销服务不可用", cause);
        };
    }
}
