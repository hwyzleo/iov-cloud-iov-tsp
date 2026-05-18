package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Ccp;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.CcpRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.CcpConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.CcpMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.CcpPo;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CcpRepositoryImpl implements CcpRepository {

    private final CcpMapper ccpMapper;
    private final CcpConverter ccpConverter;

    @Override
    public Ccp getBySn(String sn) {
        CcpPo po = ccpMapper.selectBySn(sn);
        return ccpConverter.toEntity(po);
    }

    @Override
    public int save(Ccp ccp) {
        CcpPo po = ccpConverter.toPo(ccp);
        return ccpMapper.insertPo(po);
    }

}