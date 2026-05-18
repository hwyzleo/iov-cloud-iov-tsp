package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.SimLog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.SimLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.SimConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.SimLogDao;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SimLogRepositoryImpl implements SimLogRepository {

    private final SimLogDao simLogDao;
    private final SimConverter simConverter;

    @Override
    public int save(SimLog simLog) {
        return simLogDao.insertPo(simConverter.toLogPo(simLog));
    }

    @Override
    public int deleteByIccid(String iccid) {
        return simLogDao.physicalDeletePoByIccid(iccid);
    }

}