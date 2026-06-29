package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceStatus;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 设备激活领域服务
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceActivationDomainService {

    private final TboxMapper tboxMapper;

    /**
     * 首次上线时激活设备
     * 幂等：仅当 status=PRE_ACTIVE && activate_time==null 时激活
     *
     * @param sn 设备序列号
     * @param ts 上线时间
     */
    @Transactional
    public void activateOnFirstOnline(String sn, LocalDateTime ts) {
        log.debug("尝试激活设备[{}], 上线时间[{}]", sn, ts);

        // 查询设备
        TboxPo tbox = tboxMapper.selectBySn(sn);
        if (tbox == null) {
            log.warn("设备[{}]不存在, 忽略激活", sn);
            return;
        }

        // 检查是否满足激活条件：PRE_ACTIVE 且 activate_time 为空
        if (!DeviceStatus.PRE_ACTIVE.getCode().equals(tbox.getDeviceStatus())) {
            log.debug("设备[{}]状态[{}]不是PRE_ACTIVE, 忽略激活", sn, tbox.getDeviceStatus());
            return;
        }

        if (tbox.getActivateTime() != null) {
            log.debug("设备[{}]已激活, 激活时间[{}], 忽略重复激活", sn, tbox.getActivateTime());
            return;
        }

        // 条件更新：仅当 status=PRE_ACTIVE AND activate_time IS NULL
        int updated = tboxMapper.activateDevice(sn, ts, DeviceStatus.ACTIVE.getCode());
        if (updated > 0) {
            log.info("设备[{}]激活成功, 激活时间[{}]", sn, ts);
        } else {
            log.debug("设备[{}]激活失败(可能已被其他进程激活), 忽略", sn);
        }
    }
}
