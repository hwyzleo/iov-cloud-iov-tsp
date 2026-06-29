package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.service.DeviceActivationDomainService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 车联终端事件相关应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TboxEventAppService {

    private final DeviceActivationDomainService deviceActivationDomainService;

    /**
     * 处理设备上线事件
     *
     * @param sn 设备序列号
     * @param ts 上线时间
     */
    public void handleDeviceOnline(String sn, LocalDateTime ts) {
        log.debug("处理设备上线事件, sn={}, ts={}", sn, ts);
        deviceActivationDomainService.activateOnFirstOnline(sn, ts);
    }
}
