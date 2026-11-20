package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.external.VmdVehiclePartBindingClient;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.VehicleFacts;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.DependencyUnavailableException;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.VehicleFactsPort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * VMD 车辆事实防腐适配（IOV-TSP-DSN-CR-017 D017-03）。
 * <p>
 * TSP 只使用官方绑定查询契约判断车辆在本服务视角是否「存在且可用」：
 * 无任何 ACTIVE 装配绑定的 VIN 视为不可用于绑车，返回明确失败而非放行。
 * 依赖异常与「无数据」严格区分，不 fallback 放行。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VmdVehicleFactsAdapter implements VehicleFactsPort {

    private final VmdVehiclePartBindingClient vmdVehiclePartBindingClient;

    @Override
    public VehicleFacts load(String vin) {
        try {
            List<VmdVehiclePartBindingClient.VehiclePartBindingDto> bindings =
                    vmdVehiclePartBindingClient.listActiveBindingsByVin(vin);
            if (bindings == null || bindings.isEmpty()) {
                log.info("车辆[{}]在VMD无可用装配绑定，按不可用处理", vin);
                return VehicleFacts.missing(vin);
            }
            return VehicleFacts.present(vin);
        } catch (Exception e) {
            throw new DependencyUnavailableException("VMD 车辆事实不可用: vin=" + mask(vin), e);
        }
    }

    private String mask(String vin) {
        if (vin == null || vin.length() <= 7) {
            return "***";
        }
        return vin.substring(0, 3) + "***" + vin.substring(vin.length() - 4);
    }
}
