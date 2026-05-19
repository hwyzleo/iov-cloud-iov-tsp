package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Idcm;

public interface IdcmRepository {

    Idcm getBySn(String sn);

    int save(Idcm idcm);

}