package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleNetwork;

public interface VehicleNetworkRepository {

    VehicleNetwork getById(Long id);

    VehicleNetwork getByVin(String vin);

    int save(VehicleNetwork vehicleNetwork);

    int update(VehicleNetwork vehicleNetwork);

}