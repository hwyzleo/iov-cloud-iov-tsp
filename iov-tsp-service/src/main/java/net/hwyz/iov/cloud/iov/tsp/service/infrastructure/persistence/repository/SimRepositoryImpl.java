package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Sim;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.SimRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.SimConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.SimDao;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.SimPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class SimRepositoryImpl implements SimRepository {

    private final SimDao simDao;
    private final SimConverter simConverter;

    @Override
    public Sim getById(Long id) {
        SimPo po = simDao.selectPoById(id);
        return simConverter.toEntity(po);
    }

    @Override
    public Sim getByIccid(String iccid) {
        SimPo po = simDao.selectByIccid(iccid);
        return simConverter.toEntity(po);
    }

    @Override
    public List<Sim> search(Map<String, Object> conditions) {
        List<SimPo> poList = simDao.selectPoByMap(conditions);
        return simConverter.toEntityList(poList);
    }

    @Override
    public int save(Sim sim) {
        SimPo po = simConverter.toPo(sim);
        return simDao.insertPo(po);
    }

    @Override
    public int update(Sim sim) {
        SimPo po = simConverter.toPo(sim);
        return simDao.updatePo(po);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return simDao.batchPhysicalDeletePo(ids);
    }

}