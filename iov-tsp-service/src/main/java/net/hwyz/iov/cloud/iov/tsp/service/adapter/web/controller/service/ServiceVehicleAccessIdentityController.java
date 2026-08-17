package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleAccessIdentityResultVo;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler.VehicleAccessIdentityVoAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.VehicleAccessIdentityResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.VehicleAccessIdentityAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车辆接入身份反查控制器
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/vehicleAccessIdentity/v1")
public class ServiceVehicleAccessIdentityController {

    private final VehicleAccessIdentityAppService vehicleAccessIdentityAppService;

    /**
     * 按 VIN 解析当前 TBOX 接入身份
     */
    @GetMapping("/byVin/{vin}")
    public VehicleAccessIdentityResultVo resolveByVin(@PathVariable("vin") String vin) {
        log.info("车辆接入身份反查: vin={}", mask(vin));
        VehicleAccessIdentityResult result = vehicleAccessIdentityAppService.resolveByVin(vin);
        return VehicleAccessIdentityVoAssembler.INSTANCE.toVo(result);
    }

    private String mask(String vin) {
        if (vin == null || vin.length() < 8) {
            return vin;
        }
        return vin.substring(0, 4) + "****" + vin.substring(vin.length() - 4);
    }
}
