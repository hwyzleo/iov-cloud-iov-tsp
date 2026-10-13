package net.hwyz.iov.cloud.iov.tsp.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspDeviceAdmissionService;
import net.hwyz.iov.cloud.iov.tsp.api.vo.DeviceAdmissionCheckVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.DeviceAdmissionResultVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.AdmissionDecision;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.AdmissionReason;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 设备接入准入裁决服务降级处理（TSP-DSN-CR-011 §10）
 * <p>
 * 一律 fail-closed：DENY + DEPENDENCY_UNAVAILABLE，不得降级为 ALLOW。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class TspDeviceAdmissionServiceFallbackFactory implements FallbackFactory<TspDeviceAdmissionService> {

    @Override
    public TspDeviceAdmissionService create(Throwable cause) {
        return new TspDeviceAdmissionService() {
            @Override
            public DeviceAdmissionResultVo decide(DeviceAdmissionCheckVo request) {
                log.error("设备接入准入裁决调用失败，fail-closed 拒绝", cause);
                return DeviceAdmissionResultVo.builder()
                        .decision(AdmissionDecision.DENY)
                        .reason(AdmissionReason.DEPENDENCY_UNAVAILABLE)
                        .build();
            }
        };
    }
}
