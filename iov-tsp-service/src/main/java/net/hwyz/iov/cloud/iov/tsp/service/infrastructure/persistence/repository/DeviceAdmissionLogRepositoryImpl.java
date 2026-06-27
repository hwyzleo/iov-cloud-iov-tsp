package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmission;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.DeviceAdmissionLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.DeviceAdmissionConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.DeviceAdmissionLogMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.DeviceAdmissionLogPo;
import org.springframework.stereotype.Repository;

/**
 * 设备接入鉴权日志仓储实现
 *
 * @author hwyz_leo
 * @since 2025-05-14
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DeviceAdmissionLogRepositoryImpl implements DeviceAdmissionLogRepository {

    private final DeviceAdmissionLogMapper deviceAdmissionLogMapper;
    private final DeviceAdmissionConverter deviceAdmissionConverter;

    @Override
    public void save(DeviceAdmission deviceAdmission) {
        try {
            DeviceAdmissionLogPo logPo = deviceAdmissionConverter.toLogPo(deviceAdmission);
            deviceAdmissionLogMapper.insertPo(logPo);
        } catch (Exception e) {
            log.error("保存设备接入鉴权日志失败", e);
        }
    }
}
