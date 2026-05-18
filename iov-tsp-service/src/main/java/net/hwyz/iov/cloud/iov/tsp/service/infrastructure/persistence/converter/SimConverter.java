package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.tsp.service.domain.enums.SimState;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Sim;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.SimLog;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.SimLogPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.SimPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


import java.util.List;

@Mapper(componentModel = "spring")
public interface SimConverter {

    @Mapping(target = "simState", expression = "java(simStateToInteger(sim.getSimState()))")
    @Mapping(target = "smsAbility", source = "smsAbility")
    @Mapping(target = "dataAbility", source = "dataAbility")
    @Mapping(target = "voiceAbility", source = "voiceAbility")
    SimPo toPo(Sim sim);

    @Mapping(target = "simState", expression = "java(simStateFromInteger(po.getSimState()))")
    Sim toEntity(SimPo po);

    List<Sim> toEntityList(List<SimPo> poList);

    @Mapping(target = "simState", expression = "java(simStateToInteger(simLog.getSimState()))")
    SimLogPo toLogPo(SimLog simLog);

    @Mapping(target = "simState", expression = "java(simStateFromInteger(po.getSimState()))")
    SimLog toLogEntity(SimLogPo po);

    default Integer simStateToInteger(SimState simState) {
        return simState != null ? simState.state : null;
    }

    default SimState simStateFromInteger(Integer state) {
        return SimState.valOf(state);
    }

}