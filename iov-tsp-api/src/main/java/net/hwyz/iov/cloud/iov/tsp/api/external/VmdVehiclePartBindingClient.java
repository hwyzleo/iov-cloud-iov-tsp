package net.hwyz.iov.cloud.iov.tsp.api.external;

import net.hwyz.iov.cloud.iov.tsp.api.external.fallback.VmdVehiclePartBindingClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * VMD 车辆-零件绑定关系只读查询契约（IOV-TSP-DSN-CR-017 校正）
 * <p>
 * 与 EDD-VMD 实际发布契约保持一致（{@code VmdVehiclePartBindingService}）：
 * <ul>
 *   <li>基础路径：{@code /api/service/vehiclePartBinding/v1}</li>
 *   <li>按 VIN 查 active 绑定：{@code GET /vin/{vin}/active}</li>
 *   <li>全量/增量 bootstrap：{@code GET /bootstrap}</li>
 * </ul>
 * 历史 TSP 客户端路径（{@code /api/service/vmd/v1/vehiclePartBinding/listActiveByVin}）与 VMD 实际契约不一致，
 * 调用必然 404，本次校正；降级仍抛异常，不允许以空列表冒充"无绑定"。
 *
 * @author hwyz_leo
 */
@FeignClient(name = "iov-vmd", path = "/api/service/vehiclePartBinding/v1",
        fallbackFactory = VmdVehiclePartBindingClientFallbackFactory.class)
public interface VmdVehiclePartBindingClient {

    /**
     * 按车架号查询当前全部 active 绑定
     *
     * @param vin 车架号
     * @return active 绑定列表
     */
    @GetMapping("/vin/{vin}/active")
    List<VehiclePartBindingDto> listActiveBindingsByVin(@PathVariable("vin") String vin);

    /**
     * 全量/增量分页拉取 active 绑定
     *
     * @param cursor 分页游标（首次不传）
     * @param size   每页大小
     * @return active 绑定列表
     */
    @GetMapping("/bootstrap")
    List<VehiclePartBindingDto> listBindingsForBootstrap(@RequestParam(value = "cursor", required = false) Long cursor,
                                                         @RequestParam(value = "size", defaultValue = "100") Integer size);

    /**
     * 绑定事实最小投影（字段对齐 VMD {@code VehiclePartBindingExResponse}）
     */
    record VehiclePartBindingDto(
            Long bindingId,
            String vin,
            String partCode,
            String sn,
            String deviceCategory,
            String vehicleNodeCode
    ) {
    }
}
