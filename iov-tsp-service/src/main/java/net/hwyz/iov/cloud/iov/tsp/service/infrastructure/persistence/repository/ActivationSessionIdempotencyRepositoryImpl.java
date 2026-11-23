package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingCallerScope;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.UtcTime;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationSessionIdempotency;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationSessionIdempotencyRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.ActivationSessionIdempotencyMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.ActivationSessionIdempotencyPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 车端会话创建幂等台账仓储实现（时间列按 UTC 绝对时刻显式映射）。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class ActivationSessionIdempotencyRepositoryImpl implements ActivationSessionIdempotencyRepository {

    private final ActivationSessionIdempotencyMapper activationSessionIdempotencyMapper;

    @Override
    public Optional<ActivationSessionIdempotency> find(BindingCallerScope callerScope, String vin,
                                                       String idempotencyKey) {
        if (callerScope == null || vin == null || idempotencyKey == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(activationSessionIdempotencyMapper.select(callerScope.name(), vin, idempotencyKey))
                .map(this::toEntity);
    }

    @Override
    public void insert(ActivationSessionIdempotency record) {
        ActivationSessionIdempotencyPo po = ActivationSessionIdempotencyPo.builder()
                .idempotencyId(record.getIdempotencyId())
                .callerScope(record.getCallerScope() == null ? null : record.getCallerScope().name())
                .vin(record.getVin())
                .idempotencyKey(record.getIdempotencyKey())
                .requestDigest(record.getRequestDigest())
                .sessionId(record.getSessionId())
                .expiresAt(UtcTime.toLocalDateTime(record.getExpiresAt()))
                .build();
        activationSessionIdempotencyMapper.insert(po);
        record.setId(po.getId());
    }

    @Override
    public int deleteById(Long id) {
        return id == null ? 0 : activationSessionIdempotencyMapper.deleteById(id);
    }

    private ActivationSessionIdempotency toEntity(ActivationSessionIdempotencyPo po) {
        return ActivationSessionIdempotency.builder()
                .id(po.getId())
                .idempotencyId(po.getIdempotencyId())
                .callerScope(po.getCallerScope() == null ? null : BindingCallerScope.valueOf(po.getCallerScope()))
                .vin(po.getVin())
                .idempotencyKey(po.getIdempotencyKey())
                .requestDigest(po.getRequestDigest())
                .sessionId(po.getSessionId())
                .expiresAt(UtcTime.toInstant(po.getExpiresAt()))
                .rowVersion(po.getRowVersion())
                .build();
    }
}
