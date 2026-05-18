package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleNetwork;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleNetworkLog;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleNetworkLogPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleNetworkPo;
import org.mapstruct.Mapper;


import java.util.List;

@Mapper(componentModel = "spring")
public interface VehicleNetworkConverter {

    VehicleNetworkPo toPo(VehicleNetwork vehicleNetwork);

    VehicleNetwork toEntity(VehicleNetworkPo po);

    List<VehicleNetwork> toEntityList(List<VehicleNetworkPo> poList);

    VehicleNetworkLogPo toLogPo(VehicleNetworkLog vehicleNetworkLog);

    VehicleNetworkLog toLogEntity(VehicleNetworkLogPo po);

}