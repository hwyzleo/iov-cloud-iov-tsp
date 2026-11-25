package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.PrivacyAuthorizationState;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.UtcTime;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.PrivacyAuthorization;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.PrivacyAuthorizationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.PrivacyAuthorizationMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.AccountVehiclePrivacyAuthorizationPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 隐私授权仓储实现（按来源版本收敛，不用接收时间覆盖；关系周期隔离）。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class PrivacyAuthorizationRepositoryImpl implements PrivacyAuthorizationRepository {

    private final PrivacyAuthorizationMapper privacyAuthorizationMapper;

    @Override
    public Optional<PrivacyAuthorization> find(String accountId, String vin, String relationId,
                                               String authorizationItem) {
        return Optional.ofNullable(privacyAuthorizationMapper.select(accountId, vin, relationId, authorizationItem))
                .map(po -> PrivacyAuthorization.builder()
                        .id(po.getId())
                        .authorizationId(po.getAuthorizationId())
                        .accountId(po.getAccountId())
                        .vin(po.getVin())
                        .relationId(po.getRelationId())
                        .authorizationItem(po.getAuthorizationItem())
                        .state(po.getState() == null ? null : PrivacyAuthorizationState.valueOf(po.getState()))
                        .version(po.getVersion())
                        .source(po.getSource())
                        .reportId(po.getReportId())
                        .payloadDigest(po.getPayloadDigest())
                        .reportedAt(UtcTime.toInstant(po.getReportedAt()))
                        .receivedAt(UtcTime.toInstant(po.getReceivedAt()))
                        .build());
    }

    @Override
    public void insert(PrivacyAuthorization authorization) {
        AccountVehiclePrivacyAuthorizationPo po = toPo(authorization);
        privacyAuthorizationMapper.insert(po);
        authorization.setId(po.getId());
    }

    @Override
    public boolean updateIfNewer(PrivacyAuthorization authorization) {
        return privacyAuthorizationMapper.updateIfNewer(toPo(authorization)) > 0;
    }

    private AccountVehiclePrivacyAuthorizationPo toPo(PrivacyAuthorization authorization) {
        return AccountVehiclePrivacyAuthorizationPo.builder()
                .authorizationId(authorization.getAuthorizationId())
                .accountId(authorization.getAccountId())
                .vin(authorization.getVin())
                .relationId(authorization.getRelationId())
                .authorizationItem(authorization.getAuthorizationItem())
                .state(authorization.getState() == null ? null : authorization.getState().name())
                .version(authorization.getVersion())
                .source(authorization.getSource())
                .reportId(authorization.getReportId())
                .payloadDigest(authorization.getPayloadDigest())
                .reportedAt(UtcTime.toLocalDateTime(authorization.getReportedAt()))
                .receivedAt(UtcTime.toLocalDateTime(authorization.getReceivedAt()))
                .build();
    }
}
