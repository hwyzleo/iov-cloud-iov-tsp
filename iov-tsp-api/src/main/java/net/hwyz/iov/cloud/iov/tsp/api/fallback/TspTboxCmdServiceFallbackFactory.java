package net.hwyz.iov.cloud.iov.tsp.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspTboxCmdService;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.RemoteControlRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.TboxCmdResponse;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 车联终端指令相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class TspTboxCmdServiceFallbackFactory implements FallbackFactory<TspTboxCmdService> {

    @Override
    public TspTboxCmdService create(Throwable throwable) {
        return new TspTboxCmdService() {
            @Override
            public TboxCmdResponse remoteControl(RemoteControlRequest request) {
                log.error("车联终端指令相关服务对车辆[{}]远程控制[{}]", request.getVin(), request.getType(), throwable);
                return null;
            }
        };
    }
}
