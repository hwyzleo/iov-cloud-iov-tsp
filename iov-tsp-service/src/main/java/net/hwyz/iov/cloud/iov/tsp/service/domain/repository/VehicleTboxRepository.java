package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;

public interface VehicleTboxRepository {

    VehicleTbox getByVin(String vin);

    VehicleTbox getByVinAndSn(String vin, String sn);

    int save(VehicleTbox vehicleTbox);

    int update(VehicleTbox vehicleTbox);

}