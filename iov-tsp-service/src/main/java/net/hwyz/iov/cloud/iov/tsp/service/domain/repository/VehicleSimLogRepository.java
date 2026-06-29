package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleSimLog;

/**
 * 车辆SIM卡日志仓储接口
 */
public interface VehicleSimLogRepository {
    
    /**
     * 保存日志
     */
    VehicleSimLog save(VehicleSimLog vehicleSimLog);
}
