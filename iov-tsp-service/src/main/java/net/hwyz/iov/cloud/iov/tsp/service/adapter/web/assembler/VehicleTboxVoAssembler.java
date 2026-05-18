package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleTboxVo;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.VehicleTboxBindCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.VehicleTboxResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface VehicleTboxVoAssembler {

    VehicleTboxVoAssembler INSTANCE = Mappers.getMapper(VehicleTboxVoAssembler.class);

    VehicleTboxBindCmd toBindCmd(VehicleTboxVo vo);

    VehicleTboxVo toVo(VehicleTboxResult result);

}