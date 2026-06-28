package net.hwyz.iov.cloud.iov.tsp.api.external.fallback;

import net.hwyz.iov.cloud.iov.tsp.api.external.VmdVehiclePartBindingClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class VmdVehiclePartBindingClientFallbackFactory implements FallbackFactory<VmdVehiclePartBindingClient> {

    @Override
    public VmdVehiclePartBindingClient create(Throwable cause) {
        return new VmdVehiclePartBindingClient() {
            @Override
            public List<VehiclePartBindingDto> listActiveBindingsByVin(String vin) {
                throw new RuntimeException("VMD服务不可用", cause);
            }

            @Override
            public List<VehiclePartBindingDto> listBindingsForBootstrap(Long cursor, Integer size) {
                throw new RuntimeException("VMD服务不可用", cause);
            }
        };
    }
}
