package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleNetwork;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleNetworkRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.VehicleNetworkConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleNetworkMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleNetworkPo;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class VehicleNetworkRepositoryImpl implements VehicleNetworkRepository {

    private final VehicleNetworkMapper vehicleNetworkMapper;
    private final VehicleNetworkConverter vehicleNetworkConverter;

    @Override
    public VehicleNetwork getById(Long id) {
        VehicleNetworkPo po = vehicleNetworkMapper.selectPoById(id);
        return vehicleNetworkConverter.toEntity(po);
    }

    @Override
    public VehicleNetwork getByVin(String vin) {
        VehicleNetworkPo po = vehicleNetworkMapper.selectByVin(vin);
        return vehicleNetworkConverter.toEntity(po);
    }

    @Override
    public int save(VehicleNetwork vehicleNetwork) {
        VehicleNetworkPo po = vehicleNetworkConverter.toPo(vehicleNetwork);
        return vehicleNetworkMapper.insertPo(po);
    }

    @Override
    public int update(VehicleNetwork vehicleNetwork) {
        VehicleNetworkPo po = vehicleNetworkConverter.toPo(vehicleNetwork);
        return vehicleNetworkMapper.updatePo(po);
    }

}