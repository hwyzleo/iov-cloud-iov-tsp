package net.hwyz.iov.cloud.iov.tsp.service.application.assembler;

import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.VehicleNetworkCmd;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleNetwork;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface VehicleNetworkAssembler {

    VehicleNetworkAssembler INSTANCE = Mappers.getMapper(VehicleNetworkAssembler.class);

    VehicleNetwork toEntity(VehicleNetworkCmd cmd);

}