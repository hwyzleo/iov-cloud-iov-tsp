package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;

import java.util.List;

/**
 * 车辆TBOX投影仓储接口
 */
public interface VehicleTboxProjectionRepository {

    /**
     * 根据 binding_id 幂等 upsert 投影
     */
    int upsertByBindingId(VehicleTbox vehicleTbox);

    /**
     * 根据 binding_id 查询
     */
    VehicleTbox getByBindingId(Long bindingId);

    /**
     * 获取所有活跃绑定
     */
    List<VehicleTbox> getAllActiveBindings();

    /**
     * 根据VIN查询
     */
    VehicleTbox getByVin(String vin);

    /**
     * 根据VIN和SN查询
     */
    VehicleTbox getByVinAndSn(String vin, String sn);
}
