package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleIdcmVo;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler.VehicleIdcmVoAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.VehicleIdcmBindCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.query.VehicleIdcmQuery;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.VehicleIdcmResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.VehicleIdcmAppService;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.IdcmBaseException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/vehicleIdcm/v1")
public class ServiceVehicleIdcmController {

    private final VehicleIdcmAppService vehicleIdcmAppService;

    @GetMapping("")
    public VehicleIdcmVo get(@RequestParam(required = false) String vin,
                             @RequestParam(required = false) String sn) {
        log.info("根据车架号[{}]或序列号[{}]获取车辆信息娱乐模块", vin, sn);
        if (StrUtil.isBlank(vin) && StrUtil.isBlank(sn)) {
            throw new IdcmBaseException("车架号与序列号不能都为空");
        }
        VehicleIdcmQuery query = VehicleIdcmQuery.builder()
                .vin(vin)
                .sn(sn)
                .build();
        VehicleIdcmResult result = vehicleIdcmAppService.get(query);
        return VehicleIdcmVoAssembler.INSTANCE.toVo(result);
    }

    @PostMapping("/bind")
    public void bind(@RequestBody @Validated VehicleIdcmVo vehicleIdcm) {
        log.info("车辆[{}]绑定信息娱乐模块[{}]", vehicleIdcm.getVin(), vehicleIdcm.getSn());
        VehicleIdcmBindCmd cmd = VehicleIdcmVoAssembler.INSTANCE.toBindCmd(vehicleIdcm);
        vehicleIdcmAppService.bind(cmd);
    }

}