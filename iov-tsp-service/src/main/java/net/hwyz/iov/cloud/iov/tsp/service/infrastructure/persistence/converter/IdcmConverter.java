package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Idcm;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.IdcmLog;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.IdcmLogPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.IdcmPo;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IdcmConverter {

    IdcmPo toPo(Idcm idcm);

    Idcm toEntity(IdcmPo po);

    List<Idcm> toEntityList(List<IdcmPo> poList);

    IdcmLogPo toLogPo(IdcmLog idcmLog);

}