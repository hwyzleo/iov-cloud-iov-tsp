package net.hwyz.iov.cloud.iov.tsp.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspVehicleCcpService;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleCcpVo;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 车辆中央计算平台相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class TspVehicleCcpServiceFallbackFactory implements FallbackFactory<TspVehicleCcpService> {

    @Override
    public TspVehicleCcpService create(Throwable throwable) {
        return new TspVehicleCcpService() {
            @Override
            public VehicleCcpVo get(String vin, String sn) {
                log.error("车辆中央计算平台相关服务根据车架号[{}]或序列号[{}]获取车辆中央计算平台调用异常", vin, sn, throwable);
                return null;
            }
        };
    }
}
