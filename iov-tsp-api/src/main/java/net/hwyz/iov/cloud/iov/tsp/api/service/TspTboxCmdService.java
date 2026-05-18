package net.hwyz.iov.cloud.iov.tsp.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.iov.tsp.api.fallback.TspTboxCmdServiceFallbackFactory;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.RemoteControlRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.TboxCmdResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 车联终端指令相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "tspTboxCmdService", value = ServiceNameConstants.IOV_TSP, path = "/api/service/cmd/v1", fallbackFactory = TspTboxCmdServiceFallbackFactory.class)
public interface TspTboxCmdService {

    /**
     * 远程控制
     *
     * @param request 远控请求
     * @return TBOX指令响应
     */
    @PostMapping("/action/remoteControl")
    TboxCmdResponse remoteControl(@RequestBody @Validated RemoteControlRequest request);

}
