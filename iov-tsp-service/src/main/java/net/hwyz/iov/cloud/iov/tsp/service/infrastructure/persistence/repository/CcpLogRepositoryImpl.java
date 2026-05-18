package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.CcpLog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.CcpLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.CcpConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.CcpLogMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.CcpLogPo;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CcpLogRepositoryImpl implements CcpLogRepository {

    private final CcpLogMapper ccpLogMapper;
    private final CcpConverter ccpConverter;

    @Override
    public int save(CcpLog ccpLog) {
        CcpLogPo po = ccpConverter.toLogPo(ccpLog);
        return ccpLogMapper.insertPo(po);
    }

}