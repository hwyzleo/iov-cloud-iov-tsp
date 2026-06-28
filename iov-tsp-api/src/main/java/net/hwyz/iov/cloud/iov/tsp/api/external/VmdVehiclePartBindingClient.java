package net.hwyz.iov.cloud.iov.tsp.api.external;

import net.hwyz.iov.cloud.iov.tsp.api.external.fallback.VmdVehiclePartBindingClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "iov-vmd", path = "/api/service/vmd/v1/vehiclePartBinding", fallbackFactory = VmdVehiclePartBindingClientFallbackFactory.class)
public interface VmdVehiclePartBindingClient {

    @GetMapping("/listActiveByVin")
    List<VehiclePartBindingDto> listActiveBindingsByVin(@RequestParam("vin") String vin);

    @GetMapping("/listForBootstrap")
    List<VehiclePartBindingDto> listBindingsForBootstrap(@RequestParam("cursor") Long cursor, @RequestParam("size") Integer size);

    record VehiclePartBindingDto(
        Long id,
        String vin,
        String sn,
        String partCode,
        String deviceCategory,
        String vehicleNodeCode,
        String bindState,
        Long seq,
        String occurredAt
    ) {}
}
