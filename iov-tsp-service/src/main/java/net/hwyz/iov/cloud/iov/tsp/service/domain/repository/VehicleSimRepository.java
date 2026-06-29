package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleSim;
import java.util.List;
import java.util.Optional;

/**
 * 车辆SIM卡仓储接口
 */
public interface VehicleSimRepository {
    
    /**
     * 根据ID获取
     */
    Optional<VehicleSim> getById(Long id);
    
    /**
     * 根据VIN获取所有SIM卡
     */
    List<VehicleSim> getByVin(String vin);
    
    /**
     * 根据ICCID获取
     */
    Optional<VehicleSim> getByIccid(String iccid);
    
    /**
     * 保存
     */
    VehicleSim save(VehicleSim vehicleSim);
    
    /**
     * 更新
     */
    void update(VehicleSim vehicleSim);
    
    /**
     * 条件更新（幂等）
     * 仅当 source_version 为空或 version > source_version 时更新
     * 
     * @param iccid SIM卡号
     * @param simStatus SIM卡状态
     * @param realnameStatus 实名状态
     * @param version 事件版本号
     * @return 是否更新成功
     */
    boolean updateByIccidWithVersion(String iccid, Integer simStatus, Integer realnameStatus, Long version);
    
    /**
     * 根据VIN和卡槽更新ICCID
     */
    void updateIccidByVinAndSlot(String vin, Integer slotNo, String iccid);
}
