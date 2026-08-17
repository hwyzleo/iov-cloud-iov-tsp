package net.hwyz.iov.cloud.iov.tsp.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.iov.tsp.api.fallback.TspVehicleAccessIdentityServiceFallbackFactory;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleAccessIdentityResultVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 车辆接入身份反查服务Feign接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "tspVehicleAccessIdentityService", value = ServiceNameConstants.IOV_TSP, path = "/api/service/vehicleAccessIdentity/v1", fallbackFactory = TspVehicleAccessIdentityServiceFallbackFactory.class)
public interface TspVehicleAccessIdentityService {

    /**
     * 按 VIN 解析当前 TBOX 接入身份
     *
     * @param vin 车辆VIN
     * @return 接入身份解析结果
     */
    @GetMapping("/byVin/{vin}")
    VehicleAccessIdentityResultVo resolveByVin(@PathVariable("vin") String vin);
}
