package net.hwyz.iov.cloud.iov.tsp.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.iov.tsp.api.vo.DeviceAdmissionCheckVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.DeviceAdmissionResultVo;
import net.hwyz.iov.cloud.iov.tsp.api.fallback.TspDeviceAdmissionServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 设备接入准入裁决服务Feign接口（TSP-DSN-CR-011 §4.1）
 * <p>
 * canonical 端点为 {@code POST /api/service/deviceAdmission/v1/decide}。
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "tspDeviceAdmissionService", value = ServiceNameConstants.IOV_TSP, path = "/api/service/deviceAdmission/v1", fallbackFactory = TspDeviceAdmissionServiceFallbackFactory.class)
public interface TspDeviceAdmissionService {

    /**
     * 设备接入准入裁决
     *
     * @param request 裁决请求（必须携带本次 mTLS 实际叶子证书）
     * @return 裁决结果
     */
    @PostMapping("/decide")
    DeviceAdmissionResultVo decide(@RequestBody DeviceAdmissionCheckVo request);
}
