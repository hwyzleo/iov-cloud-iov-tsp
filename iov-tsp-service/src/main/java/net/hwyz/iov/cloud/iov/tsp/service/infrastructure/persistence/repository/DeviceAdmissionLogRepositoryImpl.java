package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmission;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.DeviceAdmissionLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.metrics.DeviceAdmissionMetrics;
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
    private final DeviceAdmissionMetrics deviceAdmissionMetrics;

    @Override
    public void save(DeviceAdmission deviceAdmission) {
        try {
            DeviceAdmissionLogPo logPo = deviceAdmissionConverter.toLogPo(deviceAdmission);
            deviceAdmissionLogMapper.insertPo(logPo);
        } catch (RuntimeException e) {
            // TSP-DSN-CR-011 §9：每次准入审计 SHALL 记录，写失败不得静默。
            // 计数后向上抛出，由准入链路 fail-closed（不返回未审计的裁决）。
            deviceAdmissionMetrics.auditPersistFailed();
            log.error("保存设备接入鉴权日志失败，审计不可用，准入 fail-closed", e);
            throw e;
        }
    }
}
