package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.api.vo.CcpVo;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.CcpImportCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.CcpResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface CcpVoAssembler {

    CcpVoAssembler INSTANCE = Mappers.getMapper(CcpVoAssembler.class);

    CcpImportCmd toImportCmd(CcpVo vo);

    List<CcpImportCmd> toImportCmdList(List<CcpVo> voList);

    CcpVo toVo(CcpResult result);

}