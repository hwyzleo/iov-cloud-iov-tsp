package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.UtcTime;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleRelation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleRelationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.UserVehicleRelationMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.UserVehicleRelationPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户—车辆关系仓储实现。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class UserVehicleRelationRepositoryImpl implements UserVehicleRelationRepository {

    private final UserVehicleRelationMapper userVehicleRelationMapper;

    @Override
    public void insert(UserVehicleRelation relation) {
        UserVehicleRelationPo po = UserVehicleRelationPo.builder()
                .relationId(relation.getRelationId())
                .accountId(relation.getAccountId())
                .vin(relation.getVin())
                .role(relation.getRole())
                .state(relation.getState())
                .boundAt(UtcTime.toLocalDateTime(relation.getBoundAt()))
                .sourceOperationId(relation.getSourceOperationId())
                .ruleVersion(relation.getRuleVersion())
                .factRef(relation.getFactRef())
                .build();
        userVehicleRelationMapper.insert(po);
        relation.setId(po.getId());
    }

    @Override
    public Optional<UserVehicleRelation> findActiveByVin(String vin) {
        return Optional.ofNullable(userVehicleRelationMapper.selectActiveByVin(vin)).map(this::toEntity);
    }

    @Override
    public Optional<UserVehicleRelation> findActiveByAccountAndVin(String accountId, String vin) {
        return Optional.ofNullable(userVehicleRelationMapper.selectActiveByAccountAndVin(accountId, vin))
                .map(this::toEntity);
    }

    private UserVehicleRelation toEntity(UserVehicleRelationPo po) {
        return UserVehicleRelation.builder()
                .id(po.getId())
                .relationId(po.getRelationId())
                .accountId(po.getAccountId())
                .vin(po.getVin())
                .role(po.getRole())
                .state(po.getState())
                .boundAt(UtcTime.toInstant(po.getBoundAt()))
                .sourceOperationId(po.getSourceOperationId())
                .ruleVersion(po.getRuleVersion())
                .factRef(po.getFactRef())
                .rowVersion(po.getRowVersion())
                .build();
    }
}
