package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Tbox;

public interface TboxRepository {

    Tbox getBySn(String sn);

    int save(Tbox tbox);

}