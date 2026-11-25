package net.hwyz.iov.cloud.iov.tsp.api.external.fallback;

import net.hwyz.iov.cloud.iov.tsp.api.external.VmdVehicleActivationClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * VMD 首次激活事实只读契约降级（IOV-TSP-DSN-CR-021 D021-02）。
 * <p>
 * 不可用时抛异常，由 TSP 映射为 {@code firstActivationState=UNKNOWN} + {@code detailStatus=PARTIAL}；
 * 不允许降级为 {@code NOT_RECORDED} 或使用本地 Outbox 状态代替 VMD 事实。
 *
 * @author hwyz_leo
 */
@Component
public class VmdVehicleActivationClientFallbackFactory implements FallbackFactory<VmdVehicleActivationClient> {

    @Override
    public VmdVehicleActivationClient create(Throwable cause) {
        return vin -> {
            throw new IllegalStateException("EDD-VMD 首次激活事实查询不可用", cause);
        };
    }
}
