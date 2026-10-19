package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.hwyz.iov.cloud.iov.tsp.api.vo.event.TboxV2cKeyProvisionedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.TboxV2cKeyProvisionedConsumerMetrics;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.TboxV2cKeyProvisionedConsumerState;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.TboxV2cKeyProvisionedDltPublisher;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.TboxRegistrationAppService;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.support.TboxEventDigest;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxRegistrationEventRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleTboxProjectionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxRegistrationEventRecord;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.service.DeviceRegistrationDomainService;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspTboxV2cKeyProvisionedProperties;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxPo;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverRecord;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VMD CR-062 Outbox 事件 → TSP Consumer → ACTIVATING→ACTIVE 集成测试（TSP-DSN-CR-012 §5/§6/§10）
 * <p>
 * 装配真实 Translator + AppService + DomainService，仅替换持久化/Kafka 边界，
 * 验证：正常激活一次、重放幂等、乱序有界重试后 DLT、确定性状态立即 DLT、
 * 契约拒绝进入 DLT、提交失败后重试仅落库一次 Inbox。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TboxV2cActivationIntegrationTest {

    private static final String TOPIC = "vmd.tbox-v2c-key-provisioned";
    private static final String GROUP = "iov-tsp-tbox-v2c-key-provisioned";
    private static final String SN = "SN-0001";
    private static final String EVENT_ID = "EVT-CR012-1";
    private static final String REQUEST_ID = "REQ-CR012-1";
    private static final String VIN = "LSVAA2180C2000001";
    private static final Instant OCCURRED_AT = Instant.parse("2026-10-19T08:00:00Z");

    @Mock
    private TboxMapper tboxMapper;
    @Mock
    private VehicleTboxProjectionRepository vehicleTboxProjectionRepository;
    @Mock
    private TboxV2cKeyProvisionedDltPublisher dltPublisher;
    @Mock
    private ReceiverRecord<String, String> record;
    @Mock
    private ReceiverOffset receiverOffset;

    private TspTboxV2cKeyProvisionedProperties properties;
    private InMemoryInbox inbox;
    private AtomicInteger status;
    private TboxV2cKeyProvisionedConsumer consumer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        properties = new TspTboxV2cKeyProvisionedProperties();
        properties.setEnabled(true);
        properties.getRetry().setMaxAttempts(2);
        properties.getRetry().setInitialBackoff(Duration.ofMillis(1));
        properties.getRetry().setMaxBackoff(Duration.ofMillis(5));

        inbox = new InMemoryInbox();
        status = new AtomicInteger(DeviceStatus.ACTIVATING.getCode());

        when(tboxMapper.selectBySn(SN)).thenAnswer(invocation -> tbox(status.get()));
        when(tboxMapper.completeActivationById(anyLong(), any(), any(), any())).thenAnswer(invocation -> {
            if (status.get() == DeviceStatus.ACTIVATING.getCode()) {
                status.set(DeviceStatus.ACTIVE.getCode());
                return 1;
            }
            return 0;
        });
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN))
                .thenReturn(Collections.singletonList(VehicleTbox.builder()
                        .vin(VIN).sn(SN).deviceCategory("TBOX").bindState("ACTIVE").build()));

        DeviceRegistrationDomainService domainService =
                new DeviceRegistrationDomainService(tboxMapper, vehicleTboxProjectionRepository);
        TboxRegistrationAppService appService =
                new TboxRegistrationAppService(domainService, tboxMapper, vehicleTboxProjectionRepository, inbox);
        TboxV2cKeyProvisionedEventTranslator translator = new TboxV2cKeyProvisionedEventTranslator(
                new ObjectMapper().findAndRegisterModules(), properties);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(registry);
        TboxV2cKeyProvisionedConsumerMetrics metrics =
                new TboxV2cKeyProvisionedConsumerMetrics(new TboxV2cKeyProvisionedConsumerState(), provider);

        consumer = new TboxV2cKeyProvisionedConsumer(new KafkaProperties(), properties, translator,
                appService, dltPublisher, metrics, new TboxV2cKeyProvisionedConsumerState());

        when(record.key()).thenReturn(SN);
        when(record.value()).thenReturn(payload(SN));
        when(record.topic()).thenReturn(TOPIC);
        when(record.partition()).thenReturn(0);
        when(record.offset()).thenReturn(1L);
        when(record.receiverOffset()).thenReturn(receiverOffset);
        when(dltPublisher.publish(any(), any(), anyInt(), any(), any())).thenReturn(Mono.empty());
    }

    private String payload(String deviceSn) {
        return """
                {
                  "eventId": "EVT-CR012-1",
                  "requestId": "REQ-CR012-1",
                  "deviceSn": "%s",
                  "businessDomain": "V2C_COMM",
                  "keys": [
                    {"purpose": "UPLINK_AEAD", "keyId": "K-UP", "businessKeyVersion": 1},
                    {"purpose": "DOWNLINK_AEAD", "keyId": "K-DOWN", "businessKeyVersion": 1}
                  ],
                  "result": "SUCCEEDED",
                  "occurredAt": "2026-10-19T08:00:00Z"
                }
                """.formatted(deviceSn);
    }

    private TboxPo tbox(int deviceStatus) {
        return TboxPo.builder().id(1L).sn(SN).hsm("HSM-1").deviceStatus(deviceStatus).build();
    }

    @Test
    void consume_ValidAggregateEvent_ShouldActivateOnceAndAck() {
        consumer.handleRecord(record).block();

        assertEquals(DeviceStatus.ACTIVE.getCode(), status.get());
        assertEquals(1, inbox.records.size());
        assertEquals(DeviceRegistrationDomainService.EVENT_TYPE_V2C_KEY_PROVISIONED,
                inbox.records.get(0).eventType());
        verify(dltPublisher, never()).publish(any(), any(), anyInt(), any(), any());
        verify(receiverOffset).acknowledge();
    }

    @Test
    void consume_DuplicateReplay_ShouldBeIdempotentAndNotReactivate() {
        consumer.handleRecord(record).block();
        consumer.handleRecord(record).block();

        assertEquals(DeviceStatus.ACTIVE.getCode(), status.get());
        assertEquals(1, inbox.records.size(), "重复事件不得重复写入 Inbox");
        verify(tboxMapper, times(1)).completeActivationById(anyLong(), any(), any(), any());
        verify(receiverOffset, times(2)).acknowledge();
    }

    @Test
    void consume_PreActiveOutOfOrder_ShouldRetryThenDltWithoutActivation() {
        status.set(DeviceStatus.PRE_ACTIVE.getCode());

        consumer.handleRecord(record).block();

        assertEquals(DeviceStatus.PRE_ACTIVE.getCode(), status.get());
        assertTrue(inbox.records.isEmpty());
        ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
        verify(dltPublisher).publish(eq(record), reason.capture(), anyInt(), any(), eq(EVENT_ID));
        assertEquals("RETRY_EXHAUSTED_RETRY_OUT_OF_ORDER", reason.getValue());
        verify(receiverOffset).acknowledge();
    }

    @Test
    void consume_BlockedDevice_ShouldDltImmediatelyWithoutRetry() {
        status.set(DeviceStatus.BLOCKED.getCode());

        consumer.handleRecord(record).block();

        assertEquals(DeviceStatus.BLOCKED.getCode(), status.get());
        assertTrue(inbox.records.isEmpty());
        ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
        verify(dltPublisher).publish(eq(record), reason.capture(), eq(1), any(), eq(EVENT_ID));
        assertEquals("PERMANENT_DEVICE_STATE", reason.getValue());
        verify(tboxMapper, never()).completeActivationById(anyLong(), any(), any(), any());
        verify(receiverOffset).acknowledge();
    }

    @Test
    void consume_KafkaKeyMismatch_ShouldDltAsContractRejection() {
        when(record.key()).thenReturn("SN-OTHER");

        consumer.handleRecord(record).block();

        assertEquals(DeviceStatus.ACTIVATING.getCode(), status.get());
        assertTrue(inbox.records.isEmpty());
        ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
        verify(dltPublisher).publish(eq(record), reason.capture(), eq(1), any(), any());
        assertEquals("KEY_DEVICE_SN_MISMATCH", reason.getValue());
        verify(receiverOffset).acknowledge();
    }

    @Test
    void consume_InboxCommitFailure_ShouldRetryAndPersistOnlyOnce() {
        inbox.failNextSave = true;

        consumer.handleRecord(record).block();

        assertEquals(DeviceStatus.ACTIVE.getCode(), status.get());
        assertEquals(1, inbox.records.size());
        verify(dltPublisher, never()).publish(any(), any(), anyInt(), any(), any());
        verify(receiverOffset).acknowledge();
    }

    /**
     * 轻量 InMemory Inbox：模拟 tb_tbox_registration_event 的唯一约束与幂等查询。
     */
    private static final class InMemoryInbox implements TboxRegistrationEventRepository {

        private final List<TboxRegistrationEventRecord> records = new ArrayList<>();
        private boolean failNextSave;

        @Override
        public Optional<TboxRegistrationEventRecord> findByEventId(String eventId) {
            return records.stream().filter(r -> r.eventId().equals(eventId)).findFirst();
        }

        @Override
        public Optional<TboxRegistrationEventRecord> findByEventTypeAndRequestId(String eventType, String requestId) {
            return records.stream()
                    .filter(r -> r.eventType().equals(eventType) && requestId != null && requestId.equals(r.requestId()))
                    .findFirst();
        }

        @Override
        public List<TboxRegistrationEventRecord> listByDeviceSn(String deviceSn) {
            return records.stream().filter(r -> deviceSn.equals(r.deviceSn())).toList();
        }

        @Override
        public void save(TboxRegistrationEventRecord record) {
            if (failNextSave) {
                failNextSave = false;
                throw new IllegalStateException("simulated commit failure");
            }
            records.add(record);
        }
    }
}
