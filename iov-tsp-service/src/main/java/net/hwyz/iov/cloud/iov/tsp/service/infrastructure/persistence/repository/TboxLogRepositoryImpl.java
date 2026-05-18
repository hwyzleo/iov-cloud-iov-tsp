package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.TboxLog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.TboxConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxLogMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxLogPo;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TboxLogRepositoryImpl implements TboxLogRepository {

    private final TboxLogMapper tboxLogMapper;
    private final TboxConverter tboxConverter;

    @Override
    public int save(TboxLog tboxLog) {
        TboxLogPo po = tboxConverter.toLogPo(tboxLog);
        return tboxLogMapper.insertPo(po);
    }

}