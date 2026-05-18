package net.hwyz.iov.cloud.iov.tsp.service.application.assembler;

import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.VehicleTboxResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface VehicleTboxAssembler {

    VehicleTboxAssembler INSTANCE = Mappers.getMapper(VehicleTboxAssembler.class);

    VehicleTboxResult toResult(VehicleTbox vehicleTbox);

}