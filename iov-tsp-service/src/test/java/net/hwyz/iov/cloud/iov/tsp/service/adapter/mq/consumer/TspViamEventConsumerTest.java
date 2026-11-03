package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.ViamSecurityProjectionAppService;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TspViamInboxRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TspViamInboxRecord;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspViamProjectionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import reactor.kafka.receiver.ReceiverRecord;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VIAM 安全事件消费者单测（IOV-TSP-DSN-CR-016）
 * <p>
 * 覆盖：Inbox 幂等去重（PROCESSED 跳过）、同 eventId 契约冲突标记、
 * 成功接入事件分发、资产未导入抛可重试异常（不静默丢弃）。
 * </p>
 */
class TspViamEventConsumerTest {

    private TspViamInboxRepository inboxRepository;
    private ViamSecurityProjectionAppService projectionAppService;
    private TspViamEventConsumer consumer;

    private static final String LOGIN_TOPIC = "viam.device.login-succeeded";
    private static final String EVENT_ID = "evt-login-1";

    @BeforeEach
    void setUp() {
        inboxRepository = mock(TspViamInboxRepository.class);
        projectionAppService = mock(ViamSecurityProjectionAppService.class);
        TspViamProjectionProperties properties = new TspViamProjectionProperties();
        KafkaProperties kafkaProperties = new KafkaProperties();
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        consumer = new TspViamEventConsumer(kafkaProperties, properties, projectionAppService,
                inboxRepository, objectMapper);
    }

    private ReceiverRecord<String, String> record(String topic, String payload) {
        @SuppressWarnings("unchecked")
        ReceiverRecord<String, String> record = mock(ReceiverRecord.class);
        when(record.topic()).thenReturn(topic);
        when(record.value()).thenReturn(payload);
        when(record.key()).thenReturn("principal-1");
        when(record.offset()).thenReturn(1L);
        return record;
    }

    private String loginPayload() {
        return "{\"eventId\":\"" + EVENT_ID + "\",\"principalId\":\"principal-1\","
                + "\"hsmUid\":\"HSM-UID-1\",\"vin\":\"VIN1\",\"admissionDecisionId\":\"ad-1\","
                + "\"sessionId\":\"sess-1\",\"connectionGeneration\":1,\"certSerial\":\"AB12\","
                + "\"occurredAt\":\"2026-10-10T08:00:00Z\",\"timeSource\":\"VAGW_RECEIVED\"}";
    }

    @Test
    void alreadyProcessed_ShouldSkipDispatch() throws Exception {
        when(inboxRepository.findByEventId(EVENT_ID)).thenReturn(Optional.of(
                new TspViamInboxRecord(EVENT_ID, LOGIN_TOPIC, "DEVICE_LOGIN_SUCCEEDED", "principal-1",
                        "principal-1", "HSM-UID-1", null, "digest",
                        TspViamInboxRecord.STATUS_PROCESSED, 0, null, Instant.now(), null)));

        consumer.processOnce(record(LOGIN_TOPIC, loginPayload()));

        verify(projectionAppService, never()).handleLoginSucceeded(any(net.hwyz.iov.cloud.sec.viam.api.event.DeviceLoginSucceededEvent.class));
        verify(inboxRepository, never()).markProcessed(anyString(), any(java.time.LocalDateTime.class));
    }

    @Test
    void contractConflict_SameEventIdDifferentPayload_ShouldMarkConflict() throws Exception {
        when(inboxRepository.findByEventId(EVENT_ID)).thenReturn(Optional.of(
                new TspViamInboxRecord(EVENT_ID, LOGIN_TOPIC, "DEVICE_LOGIN_SUCCEEDED", "principal-1",
                        "principal-1", "HSM-UID-1", null, "different-digest",
                        TspViamInboxRecord.STATUS_PENDING, 0, null, Instant.now(), null)));

        consumer.processOnce(record(LOGIN_TOPIC, loginPayload()));

        verify(inboxRepository).markConflict(EVENT_ID, "CONTRACT_DIGEST_MISMATCH");
        verify(projectionAppService, never()).handleLoginSucceeded(any(net.hwyz.iov.cloud.sec.viam.api.event.DeviceLoginSucceededEvent.class));
    }

    @Test
    void loginSucceeded_ShouldSaveInboxDispatchAndMarkProcessed() throws Exception {
        when(inboxRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());

        consumer.processOnce(record(LOGIN_TOPIC, loginPayload()));

        verify(inboxRepository).save(any(TspViamInboxRecord.class));
        verify(projectionAppService).handleLoginSucceeded(any(net.hwyz.iov.cloud.sec.viam.api.event.DeviceLoginSucceededEvent.class));
        verify(inboxRepository).markProcessed(eq(EVENT_ID), any(java.time.LocalDateTime.class));
    }

    @Test
    void assetMissing_ShouldThrowRetryable_NotSilentlyDrop() throws Exception {
        when(inboxRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());
        net.hwyz.iov.cloud.sec.viam.api.event.DeviceLoginSucceededEvent event =
                new net.hwyz.iov.cloud.sec.viam.api.event.DeviceLoginSucceededEvent(
                        EVENT_ID, "principal-1", "HSM-UID-1", "VIN1", "ad-1", "sess-1", 1L,
                        "AB12", java.time.Instant.parse("2026-10-10T08:00:00Z"), "VAGW_RECEIVED");
        org.mockito.Mockito.doThrow(new ViamSecurityProjectionAppService.TboxNotFoundProjectionException(null, "HSM-UID-1"))
                .when(projectionAppService).handleLoginSucceeded(event);

        assertThrows(ViamSecurityProjectionAppService.TboxNotFoundProjectionException.class,
                () -> consumer.processOnce(record(LOGIN_TOPIC, loginPayload())));
    }
}
