package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleIdcmLog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleIdcmLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.VehicleIdcmConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleIdcmLogMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleIdcmLogPo;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class VehicleIdcmLogRepositoryImpl implements VehicleIdcmLogRepository {

    private final VehicleIdcmLogMapper vehicleIdcmLogMapper;
    private final VehicleIdcmConverter vehicleIdcmConverter;

    @Override
    public int save(VehicleIdcmLog vehicleIdcmLog) {
        VehicleIdcmLogPo po = vehicleIdcmConverter.toLogPo(vehicleIdcmLog);
        return vehicleIdcmLogMapper.insertPo(po);
    }

}