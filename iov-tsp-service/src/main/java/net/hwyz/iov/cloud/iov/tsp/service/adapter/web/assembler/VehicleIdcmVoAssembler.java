package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleIdcmVo;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.VehicleIdcmBindCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.VehicleIdcmResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface VehicleIdcmVoAssembler {

    VehicleIdcmVoAssembler INSTANCE = Mappers.getMapper(VehicleIdcmVoAssembler.class);

    VehicleIdcmBindCmd toBindCmd(VehicleIdcmVo vo);

    VehicleIdcmVo toVo(VehicleIdcmResult result);

}