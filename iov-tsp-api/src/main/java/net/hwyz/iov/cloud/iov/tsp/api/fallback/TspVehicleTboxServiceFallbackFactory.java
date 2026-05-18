package net.hwyz.iov.cloud.iov.tsp.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspVehicleTboxService;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleTboxVo;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 车辆车联终端相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class TspVehicleTboxServiceFallbackFactory implements FallbackFactory<TspVehicleTboxService> {

    @Override
    public TspVehicleTboxService create(Throwable throwable) {
        return new TspVehicleTboxService() {
            @Override
            public VehicleTboxVo get(String vin, String sn) {
                log.error("车辆车联终端相关服务根据车架号[{}]或序列号[{}]获取车辆车联终端调用异常", vin, sn, throwable);
                return null;
            }

            @Override
            public void bind(VehicleTboxVo vehicleTbox) {
                log.error("车辆车联终端相关服务车辆[{}]绑定车联终端[{}]调用异常", vehicleTbox.getVin(), vehicleTbox.getSn(), throwable);
            }
        };
    }
}
