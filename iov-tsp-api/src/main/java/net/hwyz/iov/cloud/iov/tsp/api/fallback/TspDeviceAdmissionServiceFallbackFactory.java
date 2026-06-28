package net.hwyz.iov.cloud.iov.tsp.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspDeviceAdmissionService;
import net.hwyz.iov.cloud.iov.tsp.api.vo.DeviceAdmissionCheckVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.DeviceAdmissionResultVo;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 设备接入鉴权服务降级处理
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
            public DeviceAdmissionResultVo checkDeviceAdmission(DeviceAdmissionCheckVo checkVo) {
                log.error("设备接入鉴权检查调用失败: hsm={}", checkVo.getHsm(), cause);
                DeviceAdmissionResultVo result = new DeviceAdmissionResultVo();
                result.setAdmission("DENY");
                result.setReason("服务不可用: " + cause.getMessage());
                return result;
            }
        };
    }
}
