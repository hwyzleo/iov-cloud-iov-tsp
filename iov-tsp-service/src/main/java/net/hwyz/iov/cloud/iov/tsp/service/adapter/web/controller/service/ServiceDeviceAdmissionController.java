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
 * 设备接入准入裁决控制器（TSP-DSN-CR-011 §4.1）
 * <p>
 * canonical 端点为 {@code POST /decide}；旧 {@code /check} 仅作迁移期 deprecated 别名，
 * 语义与 /decide 完全一致（缺少叶子证书时同样 fail-closed），不构成允许接入的兼容回退。
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/deviceAdmission/v1")
public class ServiceDeviceAdmissionController {

    /** 调用方标识（审计） **/
    private static final String CALLER_VAGW = "VAGW";

    private final DeviceAdmissionAppService deviceAdmissionAppService;

    /**
     * 设备接入准入裁决
     */
    @PostMapping("/decide")
    public DeviceAdmissionResultVo decide(@RequestBody @Validated DeviceAdmissionCheckVo request) {
        log.info("设备接入准入裁决请求: uidDigest={}", digest(request.getUid()));
        DeviceAdmissionCheckCmd cmd = DeviceAdmissionVoAssembler.INSTANCE.toCmd(request);
        DeviceAdmissionCheckResult result = deviceAdmissionAppService.decide(cmd, CALLER_VAGW);
        return DeviceAdmissionVoAssembler.INSTANCE.toVo(result);
    }

    /**
     * 设备接入准入裁决（迁移期 deprecated 别名，与 /decide 语义一致）
     */
    @Deprecated
    @PostMapping("/check")
    public DeviceAdmissionResultVo check(@RequestBody @Validated DeviceAdmissionCheckVo request) {
        return decide(request);
    }

    private static String digest(String value) {
        return net.hwyz.iov.cloud.iov.tsp.service.common.util.DigestUtils.sha256Hex(value);
    }
}
