package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Tbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.TboxConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxPo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class TboxRepositoryImpl implements TboxRepository {

    private final TboxMapper tboxMapper;
    private final TboxConverter tboxConverter;

    @Override
    public Tbox getBySn(String sn) {
        TboxPo po = tboxMapper.selectBySn(sn);
        return tboxConverter.toEntity(po);
    }

    @Override
    public int save(Tbox tbox) {
        TboxPo po = tboxConverter.toPo(tbox);
        return tboxMapper.insertPo(po);
    }

    @Override
    public Tbox getByHsm(String hsm) {
        TboxPo po = tboxMapper.selectByHsm(hsm);
        return tboxConverter.toEntity(po);
    }

    @Override
    public void applyViamLoginFact(Long id, LocalDateTime ts, LocalDateTime projectedAt, String syncStatus) {
        tboxMapper.applyViamLoginFact(id, ts, projectedAt, syncStatus);
    }

    @Override
    public void applyViamSnapshot(Long id, Integer deviceStatus, LocalDateTime firstLoginTime,
                                  LocalDateTime lastLoginTime, Long securityVersion,
                                  LocalDateTime projectedAt, String syncStatus) {
        tboxMapper.applyViamSnapshot(id, deviceStatus, firstLoginTime, lastLoginTime,
                securityVersion, projectedAt, syncStatus);
    }
}