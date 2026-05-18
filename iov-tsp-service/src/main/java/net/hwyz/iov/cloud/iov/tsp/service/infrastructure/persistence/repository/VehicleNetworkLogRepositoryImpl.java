package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleNetworkLog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleNetworkLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.VehicleNetworkConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleNetworkLogMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class VehicleNetworkLogRepositoryImpl implements VehicleNetworkLogRepository {

    private final VehicleNetworkLogMapper vehicleNetworkLogMapper;
    private final VehicleNetworkConverter vehicleNetworkConverter;

    @Override
    public int save(VehicleNetworkLog vehicleNetworkLog) {
        return vehicleNetworkLogMapper.insertPo(vehicleNetworkConverter.toLogPo(vehicleNetworkLog));
    }

}