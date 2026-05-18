package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTboxLog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleTboxLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.VehicleTboxConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleTboxLogMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleTboxLogPo;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class VehicleTboxLogRepositoryImpl implements VehicleTboxLogRepository {

    private final VehicleTboxLogMapper vehicleTboxLogMapper;
    private final VehicleTboxConverter vehicleTboxConverter;

    @Override
    public int save(VehicleTboxLog vehicleTboxLog) {
        VehicleTboxLogPo po = vehicleTboxConverter.toLogPo(vehicleTboxLog);
        return vehicleTboxLogMapper.insertPo(po);
    }

}