package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Ccp;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.CcpLog;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.CcpLogPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.CcpPo;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CcpConverter {

    CcpPo toPo(Ccp ccp);

    Ccp toEntity(CcpPo po);

    List<Ccp> toEntityList(List<CcpPo> poList);

    CcpLogPo toLogPo(CcpLog ccpLog);

}