package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleCcp;

import java.util.List;

/**
 * 车辆CCP投影仓储接口
 */
public interface VehicleCcpProjectionRepository {

    /**
     * 根据 binding_id 幂等 upsert 投影
     */
    int upsertByBindingId(VehicleCcp vehicleCcp);

    /**
     * 根据 binding_id 查询
     */
    VehicleCcp getByBindingId(Long bindingId);

    /**
     * 获取所有活跃绑定
     */
    List<VehicleCcp> getAllActiveBindings();

    /**
     * 根据VIN查询
     */
    VehicleCcp getByVin(String vin);

    /**
     * 根据VIN和SN查询
     */
    VehicleCcp getByVinAndSn(String vin, String sn);
}
