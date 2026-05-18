package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.api.vo.TboxVo;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.TboxImportCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.TboxResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TboxVoAssembler {

    TboxVoAssembler INSTANCE = Mappers.getMapper(TboxVoAssembler.class);

    TboxImportCmd toImportCmd(TboxVo vo);

    List<TboxImportCmd> toImportCmdList(List<TboxVo> voList);

    TboxVo toVo(TboxResult result);

}