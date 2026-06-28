package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleCcp;

import java.util.List;

public interface VehicleCcpRepository {

    VehicleCcp getByVin(String vin);

    VehicleCcp getByVinAndSn(String vin, String sn);

    int save(VehicleCcp vehicleCcp);

    int update(VehicleCcp vehicleCcp);

    /**
     * 根据 binding_id 幂等 upsert 投影
     */
    int upsertByBindingId(VehicleCcp vehicleCcp);

    /**
     * 根据 binding_id 查询
     */
    VehicleCcp getByBindingId(Long bindingId);

    /**
     * 批量获取所有绑定（用于对账）
     */
    List<VehicleCcp> getAllActiveBindings();
}
