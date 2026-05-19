package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.IdcmLog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.IdcmLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.IdcmConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.IdcmLogMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.IdcmLogPo;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IdcmLogRepositoryImpl implements IdcmLogRepository {

    private final IdcmLogMapper idcmLogMapper;
    private final IdcmConverter idcmConverter;

    @Override
    public int save(IdcmLog idcmLog) {
        IdcmLogPo po = idcmConverter.toLogPo(idcmLog);
        return idcmLogMapper.insertPo(po);
    }

}