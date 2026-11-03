package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.UtcTime;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TspViamInboxRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TspViamInboxRecord;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TspViamInboxMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TspViamInboxPo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * TSP 消费 VIAM 安全事件幂等 Inbox 仓储实现（IOV-TSP-DSN-CR-016）
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class TspViamInboxRepositoryImpl implements TspViamInboxRepository {

    private final TspViamInboxMapper tspViamInboxMapper;

    @Override
    public Optional<TspViamInboxRecord> findByEventId(String eventId) {
        return Optional.ofNullable(tspViamInboxMapper.selectByEventId(eventId)).map(this::toRecord);
    }

    @Override
    public void save(TspViamInboxRecord record) {
        try {
            tspViamInboxMapper.insert(toPo(record));
        } catch (DuplicateKeyException e) {
            // 同 eventId 并发/重放：唯一键约束收敛，视为已去重
        }
    }

    @Override
    public boolean markProcessed(String eventId, LocalDateTime processedAt) {
        return tspViamInboxMapper.markProcessed(eventId, processedAt) > 0;
    }

    @Override
    public void markRetryable(String eventId, String error) {
        tspViamInboxMapper.markRetryable(eventId, error);
    }

    @Override
    public void markConflict(String eventId, String error) {
        tspViamInboxMapper.markConflict(eventId, error);
    }

    @Override
    public void markFailed(String eventId, String error) {
        tspViamInboxMapper.markFailed(eventId, error);
    }

    private TspViamInboxPo toPo(TspViamInboxRecord record) {
        return TspViamInboxPo.builder()
                .eventId(record.eventId())
                .topic(record.topic())
                .eventType(record.eventType())
                .eventKey(record.eventKey())
                .principalId(record.principalId())
                .hsmUid(record.hsmUid())
                .deviceSn(record.deviceSn())
                .contractDigest(record.contractDigest())
                .status(record.status())
                .retryCount(record.retryCount())
                .error(record.error())
                .receivedAt(UtcTime.toLocalDateTime(record.receivedAt()))
                .processedAt(record.processedAt())
                .createTime(LocalDateTime.now())
                .build();
    }

    private TspViamInboxRecord toRecord(TspViamInboxPo po) {
        return new TspViamInboxRecord(
                po.getEventId(),
                po.getTopic(),
                po.getEventType(),
                po.getEventKey(),
                po.getPrincipalId(),
                po.getHsmUid(),
                po.getDeviceSn(),
                po.getContractDigest(),
                po.getStatus(),
                po.getRetryCount() == null ? 0 : po.getRetryCount(),
                po.getError(),
                UtcTime.toInstant(po.getReceivedAt()),
                po.getProcessedAt());
    }
}
