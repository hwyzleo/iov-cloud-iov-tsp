package net.hwyz.iov.cloud.iov.tsp.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.iov.tsp.api.vo.DeviceAdmissionCheckVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.DeviceAdmissionResultVo;
import net.hwyz.iov.cloud.iov.tsp.api.fallback.TspDeviceAdmissionServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 设备接入鉴权服务Feign接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "tspDeviceAdmissionService", value = ServiceNameConstants.IOV_TSP, path = "/api/service/deviceAdmission/v1", fallbackFactory = TspDeviceAdmissionServiceFallbackFactory.class)
public interface TspDeviceAdmissionService {

    /**
     * 设备接入鉴权检查
     *
     * @param checkVo 检查请求
     * @return 检查结果
     */
    @PostMapping("/check")
    DeviceAdmissionResultVo checkDeviceAdmission(@RequestBody DeviceAdmissionCheckVo checkVo);
}
