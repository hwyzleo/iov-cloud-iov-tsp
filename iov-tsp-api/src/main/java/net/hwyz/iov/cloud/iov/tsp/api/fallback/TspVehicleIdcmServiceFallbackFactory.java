package net.hwyz.iov.cloud.iov.tsp.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspVehicleIdcmService;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleIdcmVo;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 车辆信息娱乐模块相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class TspVehicleIdcmServiceFallbackFactory implements FallbackFactory<TspVehicleIdcmService> {

    @Override
    public TspVehicleIdcmService create(Throwable throwable) {
        return new TspVehicleIdcmService() {
            @Override
            public VehicleIdcmVo get(String vin, String sn) {
                log.error("车辆信息娱乐模块相关服务根据车架号[{}]或序列号[{}]获取车辆信息娱乐模块调用异常", vin, sn, throwable);
                return null;
            }

            @Override
            public void bind(VehicleIdcmVo vehicleIdcm) {
                log.error("车辆信息娱乐模块相关服务车辆[{}]绑定信息娱乐模块[{}]调用异常", vehicleIdcm.getVin(), vehicleIdcm.getSn(), throwable);
            }
        };
    }
}
