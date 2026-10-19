package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer;

import net.hwyz.iov.cloud.iov.tsp.api.vo.event.TboxV2cKeyProvisionedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.TboxV2cKeyProvisionedConsumerMetrics;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.TboxV2cKeyProvisionedConsumerState;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.TboxV2cKeyProvisionedContractException;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.TboxV2cKeyProvisionedDltPublisher;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.TboxV2cActivationOutcome;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.TboxRegistrationAppService;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.support.TboxEventDigest;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspTboxV2cKeyProvisionedProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverRecord;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * V2C 聚合成功事件消费者处理策略测试（TSP-DSN-CR-012 §5/§8）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TboxV2cKeyProvisionedConsumerTest {

    private static final String TOPIC = "vmd.tbox-v2c-key-provisioned";
    private static final String SN = "SN-0001";
    private static final String EVENT_ID = "EVT-1";
    private static final Instant OCCURRED_AT = Instant.parse("2026-10-19T08:00:00Z");

    @Mock
    private TboxV2cKeyProvisionedEventTranslator translator;
    @Mock
    private TboxRegistrationAppService tboxRegistrationAppService;
    @Mock
    private TboxV2cKeyProvisionedDltPublisher dltPublisher;
    @Mock
    private TboxV2cKeyProvisionedConsumerMetrics metrics;
    @Mock
    private ReceiverRecord<String, String> record;
    @Mock
    private ReceiverOffset receiverOffset;

    private TspTboxV2cKeyProvisionedProperties properties;
    private TboxV2cKeyProvisionedConsumerState state;
    private TboxV2cKeyProvisionedConsumer consumer;
    private TboxV2cKeyProvisionedEvent event;
    private String digest;

    @BeforeEach
    void setUp() {
        properties = new TspTboxV2cKeyProvisionedProperties();
        properties.getRetry().setMaxAttempts(3);
        properties.getRetry().setInitialBackoff(Duration.ofMillis(1));
        properties.getRetry().setMaxBackoff(Duration.ofMillis(5));
        state = new TboxV2cKeyProvisionedConsumerState();
        consumer = new TboxV2cKeyProvisionedConsumer(new KafkaProperties(), properties, translator,
                tboxRegistrationAppService, dltPublisher, metrics, state);

        event = TboxV2cKeyProvisionedEvent.builder()
                .eventId(EVENT_ID)
                .requestId("REQ-1")
                .deviceSn(SN)
                .businessDomain(TboxV2cKeyProvisionedEvent.BUSINESS_DOMAIN_V2C_COMM)
                .contractVersion(TboxV2cKeyProvisionedEvent.CONTRACT_VERSION_V1)
                .result(TboxV2cKeyProvisionedEvent.RESULT_SUCCEEDED)
                .occurredAt(OCCURRED_AT)
                .build();
        digest = TboxEventDigest.v2c(event);

        when(record.key()).thenReturn(SN);
        when(record.value()).thenReturn("{}");
        when(record.topic()).thenReturn(TOPIC);
        when(record.partition()).thenReturn(0);
        when(record.offset()).thenReturn(1L);
        when(record.receiverOffset()).thenReturn(receiverOffset);
        when(translator.translate(eq(TOPIC), eq(SN), eq("{}"), any(Instant.class))).thenReturn(event);
        when(dltPublisher.publish(any(), any(), anyInt(), any(), any())).thenReturn(Mono.empty());
    }

    @Test
    void buildConsumerProperties_ShouldOverrideGroupId() {
        Map<String, Object> props = consumer.buildConsumerProperties();
        assertEquals("iov-tsp-tbox-v2c-key-provisioned", props.get(ConsumerConfig.GROUP_ID_CONFIG));
    }

    @Test
    void handleRecord_Success_ShouldAckWithoutDlt() {
        when(tboxRegistrationAppService.handleV2cProvisioned(event))
                .thenReturn(TboxV2cActivationOutcome.ACTIVATED);

        consumer.handleRecord(record).block();

        verify(metrics).processed(TboxV2cActivationOutcome.ACTIVATED);
        verify(metrics).activationLatency(OCCURRED_AT);
        verify(dltPublisher, never()).publish(any(), any(), anyInt(), any(), any());
        verify(receiverOffset).acknowledge();
    }

    @Test
    void handleRecord_RetryableThenSuccess_ShouldRetryAndAck() {
        when(tboxRegistrationAppService.handleV2cProvisioned(event))
                .thenReturn(TboxV2cActivationOutcome.RETRY_OUT_OF_ORDER,
                        TboxV2cActivationOutcome.RETRY_OUT_OF_ORDER,
                        TboxV2cActivationOutcome.ACTIVATED);

        consumer.handleRecord(record).block();

        verify(metrics, times(2)).retry(TboxV2cActivationOutcome.RETRY_OUT_OF_ORDER.name());
        verify(dltPublisher, never()).publish(any(), any(), anyInt(), any(), any());
        verify(receiverOffset).acknowledge();
    }

    @Test
    void handleRecord_RetryExhausted_ShouldDltThenAck() {
        when(tboxRegistrationAppService.handleV2cProvisioned(event))
                .thenReturn(TboxV2cActivationOutcome.RETRY_OUT_OF_ORDER);

        consumer.handleRecord(record).block();

        verify(dltPublisher).publish(eq(record), eq("RETRY_EXHAUSTED_RETRY_OUT_OF_ORDER"), eq(3),
                eq(digest), eq(EVENT_ID));
        verify(metrics).dlt("RETRY_EXHAUSTED_RETRY_OUT_OF_ORDER");
        verify(receiverOffset).acknowledge();
    }

    @Test
    void handleRecord_PermanentStateRejection_ShouldDltWithoutRetry() {
        when(tboxRegistrationAppService.handleV2cProvisioned(event))
                .thenReturn(TboxV2cActivationOutcome.PERMANENT_DEVICE_STATE);

        consumer.handleRecord(record).block();

        verify(metrics, never()).retry(any());
        verify(dltPublisher).publish(eq(record), eq("PERMANENT_DEVICE_STATE"), eq(1),
                eq(digest), eq(EVENT_ID));
        verify(receiverOffset).acknowledge();
    }

    @Test
    void handleRecord_ContractRejection_ShouldDltWithoutRetry() {
        when(translator.translate(eq(TOPIC), eq(SN), eq("{}"), any(Instant.class)))
                .thenThrow(new TboxV2cKeyProvisionedContractException("KEY_DEVICE_SN_MISMATCH", "mismatch"));

        consumer.handleRecord(record).block();

        verify(metrics, never()).retry(any());
        verify(dltPublisher).publish(eq(record), eq("KEY_DEVICE_SN_MISMATCH"), eq(1), isNull(), isNull());
        verify(receiverOffset).acknowledge();
    }

    @Test
    void handleRecord_InfrastructureErrorRetryExhausted_ShouldDlt() {
        properties.getRetry().setMaxAttempts(2);
        when(translator.translate(eq(TOPIC), eq(SN), eq("{}"), any(Instant.class)))
                .thenThrow(new IllegalStateException("db down"));

        consumer.handleRecord(record).block();

        verify(metrics, times(1)).retry("INFRA_ERROR");
        verify(dltPublisher).publish(eq(record), eq("RETRY_EXHAUSTED"), eq(2), isNull(), isNull());
        verify(receiverOffset).acknowledge();
    }

    @Test
    void handleRecord_DltPublishFailure_ShouldNotAckSourceOffset() {
        when(tboxRegistrationAppService.handleV2cProvisioned(event))
                .thenReturn(TboxV2cActivationOutcome.PERMANENT_DEVICE_STATE);
        when(dltPublisher.publish(any(), any(), anyInt(), any(), any()))
                .thenReturn(Mono.error(new IllegalStateException("dlt down")));

        assertThrows(IllegalStateException.class, () -> consumer.handleRecord(record).block());

        verify(receiverOffset, never()).acknowledge();
    }
}
