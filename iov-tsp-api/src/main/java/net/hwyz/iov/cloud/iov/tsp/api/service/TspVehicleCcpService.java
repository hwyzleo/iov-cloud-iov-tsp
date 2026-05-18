package net.hwyz.iov.cloud.iov.tsp.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.iov.tsp.api.fallback.TspVehicleCcpServiceFallbackFactory;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleCcpVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 车辆中央计算平台相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "tspVehicleCcpService", value = ServiceNameConstants.IOV_TSP, path = "/api/service/vehicleCcp/v1", fallbackFactory = TspVehicleCcpServiceFallbackFactory.class)
public interface TspVehicleCcpService {

    /**
     * 根据车架号或序列号获取车辆中央计算平台
     *
     * @param vin 车架号
     * @param sn  序列号
     * @return 车辆中央计算平台
     */
    @GetMapping("")
    VehicleCcpVo get(@RequestParam(required = false) String vin, @RequestParam(required = false) String sn);

    /**
     * 车辆绑定中央计算平台
     *
     * @param vehicleCcp 车辆中央计算平台
     */
    @PostMapping("/bind")
    void bind(@RequestBody @Validated VehicleCcpVo vehicleCcp);

}
