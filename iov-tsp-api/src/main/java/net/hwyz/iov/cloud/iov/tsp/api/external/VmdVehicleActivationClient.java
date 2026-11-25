package net.hwyz.iov.cloud.iov.tsp.api.external;

import net.hwyz.iov.cloud.iov.tsp.api.external.fallback.VmdVehicleActivationClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Instant;

/**
 * VMD 整车首次激活事实只读契约（IOV-TSP-DSN-CR-021 D021-02）。
 * <p>
 * 对齐 EDD-VMD-DSN-CR-077 §7 已发布契约：
 * {@code GET /api/service/vehicleActivation/v1/vin/{vin}/firstActivation}。
 * 三态 {@code RECORDED / PENDING / NOT_RECORDED} 以 200 返回；技术失败由 VMD 以显式错误表达，
 * TSP 侧不得把故障降级为「未记录」。本客户端仅用于车辆详情受控读取，不进入列表逐行调用。
 *
 * @author hwyz_leo
 */
@FeignClient(name = "edd-vmd", contextId = "vmdVehicleActivationClient",
        path = "/api/service/vehicleActivation/v1",
        fallbackFactory = VmdVehicleActivationClientFallbackFactory.class)
public interface VmdVehicleActivationClient {

    /**
     * 查询指定 VIN 的首次激活事实（只读、幂等）
     *
     * @param vin 车架号
     * @return 三态结论 + 事实时刻
     */
    @GetMapping("/vin/{vin}/firstActivation")
    FirstActivationDto getFirstActivation(@PathVariable("vin") String vin);

    /**
     * 首次激活事实最小投影（字段对齐 VMD {@code VehicleActivationNodeExResponse}）
     *
     * @param state            RECORDED/PENDING/NOT_RECORDED
     * @param vin              车架号
     * @param firstActivatedAt 首次激活绝对时刻（UTC，非 RECORDED 为空）
     * @param factRef          事实聚合摘要（不含 PII，非 RECORDED 为空）
     */
    record FirstActivationDto(
            String state,
            String vin,
            Instant firstActivatedAt,
            String factRef
    ) {
    }
}
