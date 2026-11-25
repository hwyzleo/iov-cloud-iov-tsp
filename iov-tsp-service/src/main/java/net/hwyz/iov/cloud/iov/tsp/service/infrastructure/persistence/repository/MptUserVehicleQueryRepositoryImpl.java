package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CredentialRevocationStatus;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.UtcTime;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.MptUserVehicleHistoryCriteria;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.MptUserVehicleHistoryItem;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.MptUserVehicleQueryRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.MptUserVehicleQueryMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MPT 用户车辆关系专用只读查询仓储实现（IOV-TSP-DSN-CR-021）。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class MptUserVehicleQueryRepositoryImpl implements MptUserVehicleQueryRepository {

    private final MptUserVehicleQueryMapper mptUserVehicleQueryMapper;

    @Override
    public List<MptUserVehicleHistoryItem> searchHistory(MptUserVehicleHistoryCriteria criteria) {
        return mptUserVehicleQueryMapper.selectHistory(criteria.vin(), criteria.relationId(), criteria.operationId())
                .stream()
                .map(po -> new MptUserVehicleHistoryItem(
                        po.getRelationId(),
                        po.getVin(),
                        po.getAccountId(),
                        po.getRole(),
                        po.getState(),
                        UtcTime.toInstant(po.getBoundAt()),
                        UtcTime.toInstant(po.getReleasedAt()),
                        po.getReleaseReason(),
                        po.getReleaseOperationId(),
                        po.getReleasedBy(),
                        po.getCredentialRevocationStatus() == null ? null
                                : CredentialRevocationStatus.valueOf(po.getCredentialRevocationStatus())))
                .toList();
    }
}
