package net.hwyz.iov.cloud.iov.tsp.service.application.assembler;

import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.TboxImportCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.TboxResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Tbox;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TboxAssembler {

    TboxAssembler INSTANCE = Mappers.getMapper(TboxAssembler.class);

    TboxResult toResult(Tbox tbox);

    Tbox toEntity(TboxImportCmd cmd);

    List<Tbox> toEntityList(List<TboxImportCmd> cmdList);

}