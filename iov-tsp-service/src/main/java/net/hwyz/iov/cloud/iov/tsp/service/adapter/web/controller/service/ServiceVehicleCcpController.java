package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleCcpVo;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler.VehicleCcpExServiceAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.VehicleCcpAppService;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.CcpBaseException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 车辆中央计算平台相关服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/vehicleCcp/v1")
public class ServiceVehicleCcpController {

    private final VehicleCcpAppService vehicleCcpAppService;

    /**
     * 根据车架号或序列号获取车辆中央计算平台
     *
     * @param vin 车架号
     * @param sn  序列号
     * @return 车辆中央计算平台
     */
    @GetMapping("")
    public VehicleCcpVo get(@RequestParam(required = false) String vin, @RequestParam(required = false) String sn) {
        log.info("根据车架号[{}]或序列号[{}]获取车辆中央计算平台", vin, sn);
        if (StrUtil.isBlank(vin) && StrUtil.isBlank(sn)) {
            throw new CcpBaseException("车架号与序列号不能都为空");
        }
        return VehicleCcpExServiceAssembler.INSTANCE.fromPo(vehicleCcpAppService.get(vin, sn));
    }

    /**
     * 车辆绑定中央计算平台
     *
     * @param vehicleCcp 车辆中央计算平台
     */
    @PostMapping("/bind")
    public void bind(@RequestBody @Validated VehicleCcpVo vehicleCcp) {
        log.info("车辆[{}]绑定中央计算平台[{}]", vehicleCcp.getVin(), vehicleCcp.getSn());
        vehicleCcpAppService.bind(vehicleCcp.getVin(), vehicleCcp.getSn());
    }

}
