package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Ccp;

public interface CcpRepository {

    Ccp getBySn(String sn);

    int save(Ccp ccp);

}