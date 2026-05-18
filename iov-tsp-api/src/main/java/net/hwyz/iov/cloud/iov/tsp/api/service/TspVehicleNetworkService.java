package net.hwyz.iov.cloud.iov.tsp.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleNetworkExService;
import net.hwyz.iov.cloud.iov.tsp.api.fallback.TspVehicleNetworkServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 车辆网联相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "exVehicleNetworkService", value = ServiceNameConstants.TSP_MNO, path = "/service/vehicleNetwork", fallbackFactory = TspVehicleNetworkServiceFallbackFactory.class)
public interface TspVehicleNetworkService {

    /**
     * 创建车辆网联信息
     *
     * @param vehicleNetwork 车辆网络信息
     */
    @PostMapping("")
    void create(@RequestBody @Validated VehicleNetworkExService vehicleNetwork);

}
