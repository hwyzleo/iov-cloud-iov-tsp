package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleCcp;

import java.util.List;

public interface VehicleCcpRepository {

    VehicleCcp getByVin(String vin);

    VehicleCcp getByVinAndSn(String vin, String sn);

    int save(VehicleCcp vehicleCcp);

    int update(VehicleCcp vehicleCcp);

    int upsertByBindingId(VehicleCcp vehicleCcp);

    VehicleCcp getByBindingId(Long bindingId);

    List<VehicleCcp> getAllActiveBindings();
}
