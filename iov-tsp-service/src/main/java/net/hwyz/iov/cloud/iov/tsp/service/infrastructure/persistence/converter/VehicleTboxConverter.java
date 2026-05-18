package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTboxLog;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleTboxLogPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleTboxPo;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VehicleTboxConverter {

    VehicleTboxPo toPo(VehicleTbox vehicleTbox);

    VehicleTbox toEntity(VehicleTboxPo po);

    List<VehicleTbox> toEntityList(List<VehicleTboxPo> poList);

    VehicleTboxLogPo toLogPo(VehicleTboxLog vehicleTboxLog);

}