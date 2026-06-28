package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.DeviceAdmissionCheckVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.DeviceAdmissionResultVo;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler.DeviceAdmissionVoAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.DeviceAdmissionCheckCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.DeviceAdmissionCheckResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.DeviceAdmissionAppService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备接入鉴权控制器
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/deviceAdmission/v1")
public class ServiceDeviceAdmissionController {

    private final DeviceAdmissionAppService deviceAdmissionAppService;

    /**
     * 设备接入鉴权检查
     */
    @PostMapping("/check")
    public DeviceAdmissionResultVo checkDeviceAdmission(@RequestBody @Validated DeviceAdmissionCheckVo checkVo) {
        log.info("设备接入鉴权检查: hsm={}", checkVo.getHsm());
        DeviceAdmissionCheckCmd cmd = DeviceAdmissionVoAssembler.INSTANCE.toCmd(checkVo);
        DeviceAdmissionCheckResult result = deviceAdmissionAppService.checkAdmission(cmd);
        return DeviceAdmissionVoAssembler.INSTANCE.toVo(result);
    }
}
