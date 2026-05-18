package net.hwyz.iov.cloud.iov.tsp.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleNetworkVo;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspVehicleNetworkService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 车辆网联信息相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class TspVehicleNetworkServiceFallbackFactory implements FallbackFactory<TspVehicleNetworkService> {

    @Override
    public TspVehicleNetworkService create(Throwable throwable) {
        return new TspVehicleNetworkService() {
            @Override
            public void create(VehicleNetworkVo vehicleNetwork) {
                log.error("通讯运营商服务创建车辆[{}]网联信息调用失败", vehicleNetwork.getVin(), throwable);
            }
        };
    }
}
