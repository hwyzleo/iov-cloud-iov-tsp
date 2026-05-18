package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.SimLog;

public interface SimLogRepository {

    int save(SimLog simLog);

    int deleteByIccid(String iccid);

}