package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.UtcTime;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleAuditRecord;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleAuditRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.UserVehicleAuditMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.UserVehicleAuditPo;
import org.springframework.stereotype.Repository;

/**
 * 用户车辆关系业务审计仓储实现。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class UserVehicleAuditRepositoryImpl implements UserVehicleAuditRepository {

    private final UserVehicleAuditMapper userVehicleAuditMapper;

    @Override
    public void insert(UserVehicleAuditRecord record) {
        userVehicleAuditMapper.insert(UserVehicleAuditPo.builder()
                .auditId(record.getAuditId())
                .bizType(record.getBizType())
                .bizId(record.getBizId())
                .vin(record.getVin())
                .accountId(record.getAccountId())
                .fromState(record.getFromState())
                .toState(record.getToState())
                .operatorSource(record.getOperatorSource())
                .reason(record.getReason())
                .factRef(record.getFactRef())
                .occurredAt(UtcTime.toLocalDateTime(record.getOccurredAt()))
                .build());
    }
}
