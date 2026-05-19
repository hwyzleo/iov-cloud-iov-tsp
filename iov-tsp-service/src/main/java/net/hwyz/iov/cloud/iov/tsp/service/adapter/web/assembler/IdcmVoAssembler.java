package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.api.vo.IdcmVo;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.IdcmImportCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.IdcmResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface IdcmVoAssembler {

    IdcmVoAssembler INSTANCE = Mappers.getMapper(IdcmVoAssembler.class);

    IdcmImportCmd toImportCmd(IdcmVo vo);

    List<IdcmImportCmd> toImportCmdList(List<IdcmVo> voList);

    IdcmVo toVo(IdcmResult result);

}