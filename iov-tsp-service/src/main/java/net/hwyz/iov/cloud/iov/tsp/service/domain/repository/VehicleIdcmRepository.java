package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleIdcm;

public interface VehicleIdcmRepository {

    VehicleIdcm getByVin(String vin);

    VehicleIdcm getBySn(String sn);

    int save(VehicleIdcm vehicleIdcm);

}