package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleCcp;

import java.util.List;

/**
 * 车辆CCP投影仓储接口
 */
public interface VehicleCcpProjectionRepository {

    int upsertByBindingId(VehicleCcp vehicleCcp);

    VehicleCcp getByBindingId(Long bindingId);

    List<VehicleCcp> getAllActiveBindings();

    VehicleCcp getByVin(String vin);

    VehicleCcp getByVinAndSn(String vin, String sn);
}
