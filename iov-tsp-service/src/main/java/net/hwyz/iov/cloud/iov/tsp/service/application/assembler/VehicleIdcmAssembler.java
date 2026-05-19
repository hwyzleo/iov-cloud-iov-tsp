package net.hwyz.iov.cloud.iov.tsp.service.application.assembler;

import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.VehicleIdcmBindCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.VehicleIdcmResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleIdcm;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface VehicleIdcmAssembler {

    VehicleIdcmAssembler INSTANCE = Mappers.getMapper(VehicleIdcmAssembler.class);

    VehicleIdcmResult toResult(VehicleIdcm vehicleIdcm);

    VehicleIdcm toEntity(VehicleIdcmBindCmd cmd);

}