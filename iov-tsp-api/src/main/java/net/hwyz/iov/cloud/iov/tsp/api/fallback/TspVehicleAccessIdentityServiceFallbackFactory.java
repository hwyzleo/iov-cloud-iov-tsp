package net.hwyz.iov.cloud.iov.tsp.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspVehicleAccessIdentityService;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleAccessIdentityResultVo;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 车辆接入身份反查服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class TspVehicleAccessIdentityServiceFallbackFactory implements FallbackFactory<TspVehicleAccessIdentityService> {

    @Override
    public TspVehicleAccessIdentityService create(Throwable cause) {
        return new TspVehicleAccessIdentityService() {
            @Override
            public VehicleAccessIdentityResultVo resolveByVin(String vin) {
                log.error("车辆接入身份反查调用失败: vin={}", mask(vin), cause);
                // 基础设施异常：fail-closed，禁止返回可用于下发的伪身份
                return VehicleAccessIdentityResultVo.builder()
                        .vin(vin)
                        .status("UNRESOLVED")
                        .reason("DEPENDENCY_UNAVAILABLE")
                        .build();
            }
        };
    }

    private String mask(String vin) {
        if (vin == null || vin.length() < 8) {
            return vin;
        }
        return vin.substring(0, 4) + "****" + vin.substring(vin.length() - 4);
    }
}
