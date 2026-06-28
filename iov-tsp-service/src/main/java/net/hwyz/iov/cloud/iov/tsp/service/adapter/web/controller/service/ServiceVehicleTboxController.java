package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleTboxVo;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler.VehicleTboxVoAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.VehicleTboxResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.VehicleTboxAppService;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.TboxBaseException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/service/vehicleTbox")
public class ServiceVehicleTboxController {

    private final VehicleTboxAppService vehicleTboxAppService;

    @GetMapping("")
    public VehicleTboxVo get(@RequestParam(required = false) String vin, @RequestParam(required = false) String sn) {
        log.info("根据车架号[{}]或序列号[{}]获取车辆车联终端", vin, sn);
        if (StrUtil.isBlank(vin) && StrUtil.isBlank(sn)) {
            throw new TboxBaseException("车架号与序列号不能都为空");
        }
        VehicleTboxResult result = vehicleTboxAppService.get(vin, sn);
        return VehicleTboxVoAssembler.INSTANCE.toVo(result);
    }

}