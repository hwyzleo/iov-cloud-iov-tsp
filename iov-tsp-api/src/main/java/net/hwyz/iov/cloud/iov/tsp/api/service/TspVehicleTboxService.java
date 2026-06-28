package net.hwyz.iov.cloud.iov.tsp.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.iov.tsp.api.fallback.TspVehicleTboxServiceFallbackFactory;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleTboxVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 车辆车联终端相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "tspVehicleTboxService", value = ServiceNameConstants.IOV_TSP, path = "/service/vehicleTbox", fallbackFactory = TspVehicleTboxServiceFallbackFactory.class)
public interface TspVehicleTboxService {

    /**
     * 根据车架号或序列号获取车辆车联终端
     *
     * @param vin 车架号
     * @param sn  序列号
     * @return 车辆车联终端
     */
    @GetMapping("")
    VehicleTboxVo get(@RequestParam(required = false) String vin, @RequestParam(required = false) String sn);
}
