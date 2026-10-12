package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer;

import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.DeviceOnlineConsumerMetrics;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.DeviceOnlineConsumerState;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.DeviceOnlineContractException;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.DeviceOnlineDltPublisher;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.command.RecordTboxOnlineCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.DeviceOnlineOutcome;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.TboxRegistrationAppService;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspDeviceOnlineProperties;
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

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VagwDeviceOnlineConsumerTest {

    private static final String HSM = "HSM-UID-0001";

    @Mock
    private DeviceOnlineEventTranslator translator;
    @Mock
    private TboxRegistrationAppService tboxRegistrationAppService;
    @Mock
    private DeviceOnlineDltPublisher dltPublisher;
    @Mock
    private DeviceOnlineConsumerMetrics metrics;
    @Mock
    private ReceiverRecord<String, String> record;
    @Mock
    private ReceiverOffset receiverOffset;

    private TspDeviceOnlineProperties properties;
    private VagwDeviceOnlineConsumer consumer;

    private final RecordTboxOnlineCmd cmd = new RecordTboxOnlineCmd("EVT-1", HSM, "VIN-1", "SN-1", "S-1",
            12L, Instant.parse("2026-10-10T08:00:00Z"), "EMQX", "vagw.device.online");

    @BeforeEach
    void setUp() {
        properties = new TspDeviceOnlineProperties();
        properties.getRetry().setInitialBackoff(java.time.Duration.ofMillis(1));
        properties.getRetry().setMaxBackoff(java.time.Duration.ofMillis(5));
        consumer = new VagwDeviceOnlineConsumer(new KafkaProperties(), properties, translator,
                tboxRegistrationAppService, dltPublisher, metrics, new DeviceOnlineConsumerState());

        when(record.key()).thenReturn(HSM);
        when(record.value()).thenReturn("{}");
        when(record.topic()).thenReturn("vagw.device.online");
        when(record.partition()).thenReturn(0);
        when(record.offset()).thenReturn(1L);
        when(record.receiverOffset()).thenReturn(receiverOffset);
    }

    @Test
    void buildConsumerProperties_ShouldOverrideGroupId() {
        Map<String, Object> props = consumer.buildConsumerProperties();
        assertEquals("iov-tsp-device-online", props.get(ConsumerConfig.GROUP_ID_CONFIG));
    }

    @Test
    void handleRecord_Success_ShouldAckWithoutDlt() {
        when(translator.translate(anyString(), anyString(), any(), anyString())).thenReturn(cmd);
        when(tboxRegistrationAppService.handleOnline(cmd)).thenReturn(DeviceOnlineOutcome.PROCESSED);

        consumer.handleRecord(record).block();

        verify(receiverOffset).acknowledge();
        verify(dltPublisher, never()).publish(any(), any(), anyString(), anyInt(), any(), any());
        verify(metrics).success(DeviceOnlineOutcome.PROCESSED);
    }

    @Test
    void handleRecord_PermanentOutcome_ShouldDltThenAck() {
        when(translator.translate(anyString(), anyString(), any(), anyString())).thenReturn(cmd);
        when(tboxRegistrationAppService.handleOnline(cmd)).thenReturn(DeviceOnlineOutcome.PERMANENT_BINDING_CONFLICT);
        when(dltPublisher.publish(any(), any(), anyString(), anyInt(), any(), any())).thenReturn(Mono.empty());

        consumer.handleRecord(record).block();

        verify(dltPublisher).publish(eq(record), any(), eq("PERMANENT_BINDING_CONFLICT"), anyInt(), any(), any());
        verify(receiverOffset).acknowledge();
    }

    @Test
    void handleRecord_ContractError_ShouldDltThenAck() {
        when(translator.translate(anyString(), anyString(), any(), anyString()))
                .thenThrow(new DeviceOnlineContractException("INVALID_EVENT_TYPE", "bad"));
        when(dltPublisher.publish(any(), any(), anyString(), anyInt(), any(), any())).thenReturn(Mono.empty());

        consumer.handleRecord(record).block();

        verify(dltPublisher).publish(eq(record), eq(HSM), eq("INVALID_EVENT_TYPE"), anyInt(), any(), any());
        verify(receiverOffset).acknowledge();
    }

    @Test
    void handleRecord_RetryableExhausted_ShouldDltThenAck() {
        properties.getRetry().setMaxAttempts(1);
        when(translator.translate(anyString(), anyString(), any(), anyString())).thenReturn(cmd);
        when(tboxRegistrationAppService.handleOnline(cmd)).thenReturn(DeviceOnlineOutcome.RETRY_TBOX_NOT_FOUND);
        when(dltPublisher.publish(any(), any(), anyString(), anyInt(), any(), any())).thenReturn(Mono.empty());

        consumer.handleRecord(record).block();

        verify(dltPublisher).publish(eq(record), any(),
                argThat(code -> code.startsWith("RETRY_EXHAUSTED")), anyInt(), any(), any());
        verify(receiverOffset).acknowledge();
    }

    @Test
    void handleRecord_DltFailure_ShouldNotAck() {
        when(translator.translate(anyString(), anyString(), any(), anyString())).thenReturn(cmd);
        when(tboxRegistrationAppService.handleOnline(cmd)).thenReturn(DeviceOnlineOutcome.PERMANENT_BINDING_CONFLICT);
        when(dltPublisher.publish(any(), any(), anyString(), anyInt(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException("dlt down")));

        assertThrows(RuntimeException.class, () -> consumer.handleRecord(record).block());
        verify(receiverOffset, never()).acknowledge();
    }
}
