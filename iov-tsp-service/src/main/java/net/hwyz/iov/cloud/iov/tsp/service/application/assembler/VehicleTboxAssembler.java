package net.hwyz.iov.cloud.iov.tsp.service.application.assembler;

import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.VehicleTboxResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface VehicleTboxAssembler {

    VehicleTboxAssembler INSTANCE = org.mapstruct.factory.Mappers.getMapper(VehicleTboxAssembler.class);

    @Mapping(source = "createTime", target = "createTime")
    VehicleTboxResult toResult(VehicleTbox vehicleTbox);
}
