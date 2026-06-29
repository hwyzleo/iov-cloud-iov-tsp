package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleSim;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleSimLog;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleSimPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleSimLogPo;

/**
 * 车辆SIM卡转换器
 */
@Mapper(componentModel = "spring")
public interface VehicleSimConverter {
    
    /**
     * PO 转 Entity
     */
    VehicleSim toEntity(VehicleSimPo po);
    
    /**
     * Entity 转 PO
     */
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "modifyTime", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "rowValid", ignore = true)
    VehicleSimPo toPo(VehicleSim entity);
    
    /**
     * Entity 转 Log Entity
     */
    VehicleSimLog toLogEntity(VehicleSim entity);
    
    /**
     * Log Entity 转 Log PO
     */
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "modifyTime", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "rowValid", ignore = true)
    VehicleSimLogPo toLogPo(VehicleSimLog entity);
}