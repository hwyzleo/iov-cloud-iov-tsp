package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Idcm;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.IdcmRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.IdcmConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.IdcmMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.IdcmPo;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IdcmRepositoryImpl implements IdcmRepository {

    private final IdcmMapper idcmMapper;
    private final IdcmConverter idcmConverter;

    @Override
    public Idcm getBySn(String sn) {
        IdcmPo po = idcmMapper.selectBySn(sn);
        return idcmConverter.toEntity(po);
    }

    @Override
    public int save(Idcm idcm) {
        IdcmPo po = idcmConverter.toPo(idcm);
        return idcmMapper.insertPo(po);
    }

}