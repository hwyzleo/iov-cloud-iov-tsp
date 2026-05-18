package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.api.vo.SimExService;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.SimImportCmd;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface SimExServiceAssembler {

    SimExServiceAssembler INSTANCE = Mappers.getMapper(SimExServiceAssembler.class);

    SimImportCmd toCmd(SimExService vo);

    List<SimImportCmd> toCmdList(List<SimExService> voList);

}