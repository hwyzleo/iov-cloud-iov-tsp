package net.hwyz.iov.cloud.iov.tsp.service.application.assembler;

import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.CcpImportCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.CcpResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Ccp;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface CcpAssembler {

    CcpAssembler INSTANCE = Mappers.getMapper(CcpAssembler.class);

    CcpResult toResult(Ccp ccp);

    Ccp toEntity(CcpImportCmd cmd);

    List<Ccp> toEntityList(List<CcpImportCmd> cmdList);

}