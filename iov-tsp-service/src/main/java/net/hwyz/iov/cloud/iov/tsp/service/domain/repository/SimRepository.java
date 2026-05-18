package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Sim;

import java.util.List;
import java.util.Map;

public interface SimRepository {

    Sim getById(Long id);

    Sim getByIccid(String iccid);

    List<Sim> search(Map<String, Object> conditions);

    int save(Sim sim);

    int update(Sim sim);

    int deleteByIds(Long[] ids);

}