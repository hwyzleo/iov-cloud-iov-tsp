package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleNetworkExService;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.VehicleNetworkAppService;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler.VehicleNetworkExServiceAssembler;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车辆网联信息相关服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/service/vehicleNetwork")
public class ServiceVehicleNetworkController {

    private final VehicleNetworkAppService vehicleNetworkAppService;

    /**
     * 创建车辆网联信息
     *
     * @param vehicleNetwork 车辆网络信息
     */
    @PostMapping("")
    public void create(@RequestBody @Validated VehicleNetworkExService vehicleNetwork) {
        log.info("创建车辆[{}]网联信息", vehicleNetwork.getVin());
        vehicleNetworkAppService.create(VehicleNetworkExServiceAssembler.INSTANCE.toPo(vehicleNetwork));
    }

}
