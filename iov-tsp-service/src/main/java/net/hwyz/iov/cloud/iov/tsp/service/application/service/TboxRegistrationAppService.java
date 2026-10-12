package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.event.TboxDeviceOnlineEvent;
import net.hwyz.iov.cloud.iov.tsp.api.vo.event.TboxV2cKeyProvisionedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.service.DeviceRegistrationDomainService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * TBOX 注册激活应用服务
 * <p>
 * TSP-DSN-CR-008：编排设备登录记录与 V2C 业务激活完成。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TboxRegistrationAppService {

    private final DeviceRegistrationDomainService deviceRegistrationDomainService;

    /**
     * 处理设备上线事件：记录首次/最近登录时间并驱动 PRE_ACTIVE -> ACTIVATING
     *
     * @param event TBOX 设备上线事件
     */
    public void handleOnline(TboxDeviceOnlineEvent event) {
        if (event == null) {
            log.warn("收到空的 DEVICE_ONLINE 事件，忽略");
            return;
        }
        LocalDateTime ts = event.getTs() != null
                ? LocalDateTime.ofInstant(event.getTs(), ZoneId.systemDefault())
                : LocalDateTime.now();
        log.debug("处理设备上线事件, eventId={}, hsmUid={}, tboxSn={}, ts={}",
                event.getEventId(), event.getHsmUid(), event.getTboxSn(), ts);
        deviceRegistrationDomainService.recordSuccessfulLogin(event.getEventId(), event.getHsmUid(), event.getTboxSn(), ts);
    }

    /**
     * 处理 VMD V2C 业务密钥开通结果事件：驱动 ACTIVATING -> ACTIVE
     *
     * @param event VMD V2C 密钥开通结果事件
     * @return 是否完成激活
     */
    public boolean handleV2cProvisioned(TboxV2cKeyProvisionedEvent event) {
        log.debug("处理 V2C 密钥开通结果事件, requestId={}",
                event != null ? event.getRequestId() : null);
        return deviceRegistrationDomainService.completeActivation(event);
    }
}
