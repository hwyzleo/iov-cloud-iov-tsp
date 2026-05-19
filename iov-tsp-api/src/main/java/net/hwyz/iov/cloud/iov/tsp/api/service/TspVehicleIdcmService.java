package net.hwyz.iov.cloud.iov.tsp.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.iov.tsp.api.fallback.TspVehicleIdcmServiceFallbackFactory;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleIdcmVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 车辆信息娱乐模块相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "tspVehicleIdcmService", value = ServiceNameConstants.IOV_TSP, path = "/api/service/vehicleIdcm/v1", fallbackFactory = TspVehicleIdcmServiceFallbackFactory.class)
public interface TspVehicleIdcmService {

    /**
     * 根据车架号或序列号获取车辆信息娱乐模块
     *
     * @param vin 车架号
     * @param sn  序列号
     * @return 车辆信息娱乐模块
     */
    @GetMapping("")
    VehicleIdcmVo get(@RequestParam(required = false) String vin, @RequestParam(required = false) String sn);

    /**
     * 车辆绑定信息娱乐模块
     *
     * @param vehicleIdcm 车辆信息娱乐模块
     */
    @PostMapping("/bind")
    void bind(@RequestBody @Validated VehicleIdcmVo vehicleIdcm);

}
