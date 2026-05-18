package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleNetworkVo;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler.VehicleNetworkExServiceAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.VehicleNetworkCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.VehicleNetworkAppService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/vehicleNetwork/v1")
public class ServiceVehicleNetworkController {

    private final VehicleNetworkAppService vehicleNetworkAppService;

    @PostMapping("")
    public void create(@RequestBody @Validated VehicleNetworkVo vehicleNetwork) {
        log.info("创建车辆[{}]网联信息", vehicleNetwork.getVin());
        VehicleNetworkCmd cmd = VehicleNetworkExServiceAssembler.INSTANCE.toCmd(vehicleNetwork);
        vehicleNetworkAppService.create(cmd);
    }

}