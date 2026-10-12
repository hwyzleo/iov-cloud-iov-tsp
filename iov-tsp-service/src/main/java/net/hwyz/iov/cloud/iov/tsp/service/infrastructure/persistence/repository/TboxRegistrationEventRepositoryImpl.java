package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxRegistrationEventRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxRegistrationEventRecord;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxRegistrationEventMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxRegistrationEventPo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * TBOX 注册激活事件消费记录仓储实现
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class TboxRegistrationEventRepositoryImpl implements TboxRegistrationEventRepository {

    private final TboxRegistrationEventMapper tboxRegistrationEventMapper;

    @Override
    public Optional<TboxRegistrationEventRecord> findByEventId(String eventId) {
        return Optional.ofNullable(tboxRegistrationEventMapper.selectByEventId(eventId)).map(this::toRecord);
    }

    @Override
    public Optional<TboxRegistrationEventRecord> findByEventTypeAndRequestId(String eventType, String requestId) {
        return Optional.ofNullable(tboxRegistrationEventMapper.selectByEventTypeAndRequestId(eventType, requestId))
                .map(this::toRecord);
    }

    @Override
    public List<TboxRegistrationEventRecord> listByDeviceSn(String deviceSn) {
        return tboxRegistrationEventMapper.selectByDeviceSn(deviceSn).stream()
                .map(this::toRecord)
                .toList();
    }

    @Override
    public void save(TboxRegistrationEventRecord record) {
        tboxRegistrationEventMapper.insert(TboxRegistrationEventPo.builder()
                .eventId(record.eventId())
                .requestId(record.requestId())
                .deviceSn(record.deviceSn())
                .eventType(record.eventType())
                .occurredAt(toLocalDateTime(record.occurredAt()))
                .processedAt(record.processedAt())
                .result(record.result())
                .contractDigest(record.contractDigest())
                .sourceTopic(record.sourceTopic())
                .connectionGeneration(record.connectionGeneration())
                .createTime(LocalDateTime.now())
                .build());
    }

    private TboxRegistrationEventRecord toRecord(TboxRegistrationEventPo po) {
        return new TboxRegistrationEventRecord(
                po.getEventId(),
                po.getRequestId(),
                po.getDeviceSn(),
                po.getEventType(),
                po.getOccurredAt() != null
                        ? po.getOccurredAt().atZone(ZoneId.systemDefault()).toInstant()
                        : null,
                po.getProcessedAt(),
                po.getResult(),
                po.getContractDigest(),
                po.getSourceTopic(),
                po.getConnectionGeneration());
    }

    private LocalDateTime toLocalDateTime(java.time.Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
