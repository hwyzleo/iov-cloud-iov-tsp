package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmission;

/**
 * 设备接入鉴权日志仓储接口
 *
 * @author hwyz_leo
 */
public interface DeviceAdmissionLogRepository {

    /**
     * 保存鉴权日志
     * @param deviceAdmission 设备接入鉴权实体
     */
    void save(DeviceAdmission deviceAdmission);
}
