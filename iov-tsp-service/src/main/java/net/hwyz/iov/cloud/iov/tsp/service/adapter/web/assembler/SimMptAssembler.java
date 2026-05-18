package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.vo.SimMpt;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.SimCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.query.SimQuery;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.SimResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface SimMptAssembler {

    SimMptAssembler INSTANCE = Mappers.getMapper(SimMptAssembler.class);

    SimMpt toVo(SimResult result);

    List<SimMpt> toVoList(List<SimResult> resultList);

    SimCmd toCmd(SimMpt vo);

    SimQuery toQuery(SimMpt vo);

}