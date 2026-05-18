package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleNetworkExService;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.VehicleNetworkCmd;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface VehicleNetworkExServiceAssembler {

    VehicleNetworkExServiceAssembler INSTANCE = Mappers.getMapper(VehicleNetworkExServiceAssembler.class);

    VehicleNetworkCmd toCmd(VehicleNetworkExService vo);

}