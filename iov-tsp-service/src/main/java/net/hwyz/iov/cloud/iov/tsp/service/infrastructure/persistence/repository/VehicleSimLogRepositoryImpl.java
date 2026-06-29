package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleSimLog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleSimLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.VehicleSimConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleSimLogMapper;

/**
 * 车辆SIM卡日志仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehicleSimLogRepositoryImpl implements VehicleSimLogRepository {
    
    private final VehicleSimLogMapper vehicleSimLogMapper;
    private final VehicleSimConverter vehicleSimConverter;
    
    @Override
    public VehicleSimLog save(VehicleSimLog vehicleSimLog) {
        var po = vehicleSimConverter.toLogPo(vehicleSimLog);
        vehicleSimLogMapper.insert(po);
        vehicleSimLog.setId(po.getId());
        return vehicleSimLog;
    }
}