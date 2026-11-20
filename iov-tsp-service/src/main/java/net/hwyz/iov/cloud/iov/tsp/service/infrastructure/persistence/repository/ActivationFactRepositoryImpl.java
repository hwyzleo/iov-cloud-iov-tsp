package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.UtcTime;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationFact;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationFactRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.ActivationFactMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.ActivationFactPo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 首次激活候选事实与 Outbox 仓储实现。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class ActivationFactRepositoryImpl implements ActivationFactRepository {

    private final ActivationFactMapper activationFactMapper;

    @Override
    public Optional<ActivationFact> findByEventId(String eventId) {
        return Optional.ofNullable(activationFactMapper.selectByEventId(eventId)).map(this::toEntity);
    }

    @Override
    public Optional<ActivationFact> findByBusinessKey(String vin, String operationId) {
        return Optional.ofNullable(activationFactMapper.selectByBusinessKey(vin, operationId)).map(this::toEntity);
    }

    @Override
    public void insert(ActivationFact fact) {
        ActivationFactPo po = ActivationFactPo.builder()
                .eventId(fact.getEventId())
                .schemaVersion(fact.getSchemaVersion())
                .vin(fact.getVin())
                .bindingOperationId(fact.getBindingOperationId())
                .sourceFactRef(fact.getSourceFactRef())
                .occurredAt(UtcTime.toLocalDateTime(fact.getOccurredAt()))
                .payloadDigest(fact.getPayloadDigest())
                .publishStatus(fact.getPublishStatus())
                .build();
        activationFactMapper.insert(po);
        fact.setId(po.getId());
    }

    @Override
    public List<ActivationFact> findPending(int limit) {
        return activationFactMapper.selectPending(limit).stream().map(this::toEntity).toList();
    }

    @Override
    public int markPublished(String eventId, Instant now) {
        return activationFactMapper.markPublished(eventId, UtcTime.toLocalDateTime(now));
    }

    @Override
    public int markFailed(String eventId, String error, String status, Instant now) {
        return activationFactMapper.markFailed(eventId, error, status, UtcTime.toLocalDateTime(now));
    }

    private ActivationFact toEntity(ActivationFactPo po) {
        return ActivationFact.builder()
                .id(po.getId())
                .eventId(po.getEventId())
                .schemaVersion(po.getSchemaVersion())
                .vin(po.getVin())
                .bindingOperationId(po.getBindingOperationId())
                .sourceFactRef(po.getSourceFactRef())
                .occurredAt(UtcTime.toInstant(po.getOccurredAt()))
                .payloadDigest(po.getPayloadDigest())
                .publishStatus(po.getPublishStatus())
                .retryCount(po.getRetryCount())
                .lastError(po.getLastError())
                .publishedAt(UtcTime.toInstant(po.getPublishedAt()))
                .build();
    }
}
