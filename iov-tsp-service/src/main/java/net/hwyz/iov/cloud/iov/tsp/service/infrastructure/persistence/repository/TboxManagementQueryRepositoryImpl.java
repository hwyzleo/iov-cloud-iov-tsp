package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxManagementQueryRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxManagementCriteria;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxManagementView;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxManagementQueryMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TBOX 后台运营查询仓储实现（只读）
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class TboxManagementQueryRepositoryImpl implements TboxManagementQueryRepository {

    private final TboxManagementQueryMapper tboxManagementQueryMapper;

    @Override
    public List<TboxManagementView> search(TboxManagementCriteria criteria) {
        return tboxManagementQueryMapper.selectPageByCriteria(criteria);
    }

    @Override
    public TboxManagementView getBySn(String sn) {
        return tboxManagementQueryMapper.selectBySn(sn);
    }
}
