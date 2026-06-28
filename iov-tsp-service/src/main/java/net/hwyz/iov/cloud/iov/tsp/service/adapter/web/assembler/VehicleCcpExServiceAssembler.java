package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleCcpVo;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleCcp;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleCcpPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface VehicleCcpExServiceAssembler {

    VehicleCcpExServiceAssembler INSTANCE = Mappers.getMapper(VehicleCcpExServiceAssembler.class);

    VehicleCcpVo fromPo(VehicleCcpPo vehicleCcpPo);

    VehicleCcpVo fromEntity(VehicleCcp vehicleCcp);

    VehicleCcpPo toPo(VehicleCcpVo vehicleCcpVo);
}
