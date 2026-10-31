package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxRegistrationEventRecord;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxRegistrationEventMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxRegistrationEventPo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TSP-DSN-CR-014：Inbox（tb_tbox_registration_event）occurred_at 显式 UTC 存取测试。
 * <p>
 * 验证：Instant（绝对时刻）→ DATETIME 墙钟按 UTC 写入；读回按 UTC 解释为同一 Instant。
 * 不依赖 JVM 默认时区（旧实现用 ZoneId.systemDefault()，JVM 时区变更即失真）。
 *
 * @author hwyz_leo
 * @since 2026-10-31
 */
@ExtendWith(MockitoExtension.class)
class TboxRegistrationEventRepositoryImplTest {

    private static final String EVENT_ID = "EVT-1";
    private static final String DEVICE_SN = "SN-1";
    private static final Instant OCCURRED_AT = Instant.parse("2026-10-10T08:00:00Z");
    private static final LocalDateTime OCCURRED_AT_UTC = LocalDateTime.ofInstant(OCCURRED_AT, ZoneOffset.UTC);

    @Mock
    private TboxRegistrationEventMapper tboxRegistrationEventMapper;

    @InjectMocks
    private TboxRegistrationEventRepositoryImpl repository;

    @Test
    void save_ShouldWriteOccurredAtAsUtcWallClock() {
        TboxRegistrationEventRecord record = new TboxRegistrationEventRecord(
                EVENT_ID, null, DEVICE_SN, "DEVICE_ONLINE",
                OCCURRED_AT, LocalDateTime.of(2026, 10, 10, 8, 0, 1), "SUCCEEDED",
                "digest", "vagw.device.online", 7L);

        repository.save(record);

        ArgumentCaptor<TboxRegistrationEventPo> captor = ArgumentCaptor.forClass(TboxRegistrationEventPo.class);
        verify(tboxRegistrationEventMapper).insert(captor.capture());
        TboxRegistrationEventPo po = captor.getValue();
        assertEquals(OCCURRED_AT_UTC, po.getOccurredAt(),
                "occurred_at 必须以 UTC 墙钟写入（旧实现为 systemDefault 墙钟）");
    }

    @Test
    void findByEventId_ShouldInterpretOccurredAtAsUtc() {
        TboxRegistrationEventPo po = TboxRegistrationEventPo.builder()
                .eventId(EVENT_ID)
                .deviceSn(DEVICE_SN)
                .eventType("DEVICE_ONLINE")
                .occurredAt(OCCURRED_AT_UTC)
                .processedAt(LocalDateTime.of(2026, 10, 10, 8, 0, 1))
                .result("SUCCEEDED")
                .contractDigest("digest")
                .build();
        when(tboxRegistrationEventMapper.selectByEventId(anyString())).thenReturn(po);

        Optional<TboxRegistrationEventRecord> record = repository.findByEventId(EVENT_ID);

        assertEquals(OCCURRED_AT, record.orElseThrow().occurredAt(),
                "读回必须按 UTC 解释为同一绝对时刻（旧实现为 systemDefault 解释）");
    }
}
