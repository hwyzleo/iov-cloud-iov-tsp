package net.hwyz.iov.cloud.iov.tsp.service.application.assembler;

import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.SimCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.SimImportCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.SimResult;
import net.hwyz.iov.cloud.iov.tsp.service.common.enums.SimState;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Sim;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface SimAssembler {

    SimAssembler INSTANCE = Mappers.getMapper(SimAssembler.class);

    @Mapping(target = "simState", expression = "java(simStateToInteger(sim.getSimState()))")
    SimResult toResult(Sim sim);

    List<SimResult> toResultList(List<Sim> simList);

    @Mapping(target = "simState", expression = "java(simStateFromInteger(cmd.getSimState()))")
    Sim toEntity(SimCmd cmd);

    @Mapping(target = "simState", ignore = true)
    @Mapping(target = "smsAbility", ignore = true)
    @Mapping(target = "dataAbility", ignore = true)
    @Mapping(target = "voiceAbility", ignore = true)
    Sim toEntity(SimImportCmd cmd);

    List<Sim> toEntityList(List<SimImportCmd> cmdList);

    default Integer simStateToInteger(SimState simState) {
        return simState != null ? simState.state : null;
    }

    default SimState simStateFromInteger(Integer state) {
        return SimState.valOf(state);
    }

}