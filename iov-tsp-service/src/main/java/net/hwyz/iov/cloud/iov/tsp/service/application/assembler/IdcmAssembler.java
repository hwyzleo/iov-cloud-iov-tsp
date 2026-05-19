package net.hwyz.iov.cloud.iov.tsp.service.application.assembler;

import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.IdcmImportCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.IdcmResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Idcm;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface IdcmAssembler {

    IdcmAssembler INSTANCE = Mappers.getMapper(IdcmAssembler.class);

    IdcmResult toResult(Idcm idcm);

    Idcm toEntity(IdcmImportCmd cmd);

    List<Idcm> toEntityList(List<IdcmImportCmd> cmdList);

}