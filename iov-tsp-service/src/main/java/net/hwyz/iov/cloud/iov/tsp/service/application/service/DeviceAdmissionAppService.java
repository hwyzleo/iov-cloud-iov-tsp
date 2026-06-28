package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.application.assembler.DeviceAdmissionAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.DeviceAdmissionCheckCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.DeviceAdmissionCheckResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmission;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.service.DeviceAdmissionDomainService;
import org.springframework.stereotype.Service;

/**
 * 设备接入鉴权应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceAdmissionAppService {

    private final DeviceAdmissionDomainService deviceAdmissionDomainService;
    private final DeviceAdmissionAssembler deviceAdmissionAssembler;

    /**
     * 执行设备接入鉴权检查
     * @param cmd 检查命令
     * @return 检查结果
     */
    public DeviceAdmissionCheckResult checkAdmission(DeviceAdmissionCheckCmd cmd) {
        log.info("执行设备接入鉴权检查: hsm={}, vin={}", cmd.getHsm(), cmd.getVin());

        DeviceAdmission deviceAdmission = deviceAdmissionDomainService.checkAdmission(
            cmd.getHsm(), cmd.getVin());

        return deviceAdmissionAssembler.toResult(deviceAdmission);
    }
}
