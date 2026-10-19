package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspTboxV2cKeyProvisionedProperties;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.sender.SenderResult;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * V2C 事件 DLT 发布器测试（TSP-DSN-CR-012 §8/§10-8）
 * <p>
 * 校验 DLT 只带非敏感诊断 header，不新增 keyId/密码学材料。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TboxV2cKeyProvisionedDltPublisherTest {

    private static final String SN = "SN-0001";
    private static final String PAYLOAD = "{\"eventId\":\"EVT-1\",\"keys\":[{\"keyId\":\"K-UP\"}]}";

    @Mock
    private ReactiveKafkaProducerTemplate<String, String> template;
    @Mock
    private SenderResult<Void> senderResult;
    @Mock
    private ReceiverRecord<String, String> record;

    private TspTboxV2cKeyProvisionedProperties properties;
    private TboxV2cKeyProvisionedDltPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new TspTboxV2cKeyProvisionedProperties();
        publisher = new TboxV2cKeyProvisionedDltPublisher(template, properties);
        when(record.key()).thenReturn(SN);
        when(record.value()).thenReturn(PAYLOAD);
        when(record.topic()).thenReturn("vmd.tbox-v2c-key-provisioned");
        when(record.partition()).thenReturn(2);
        when(record.offset()).thenReturn(99L);
        when(template.send(any(ProducerRecord.class))).thenReturn(Mono.just(senderResult));
    }

    @Test
    void publish_ShouldWriteDiagnosticHeadersWithoutKeyId() {
        publisher.publish(record, "PERMANENT_DEVICE_STATE", 2, "digest-1", "EVT-1").block();

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(template).send(captor.capture());
        ProducerRecord<String, String> sent = captor.getValue();

        assertEquals("tsp.tbox-v2c-key-provisioned.dlt", sent.topic());
        assertEquals(SN, sent.key());
        assertEquals(PAYLOAD, sent.value());

        List<String> headerKeys = Arrays.stream(sent.headers().toArray()).map(Header::key).toList();
        assertTrue(headerKeys.containsAll(List.of("sourceTopic", "sourcePartition", "sourceOffset", "consumerGroup",
                "errorCode", "failedAt", "retryCount", "contractDigest", "eventId")));
        assertFalse(headerKeys.contains("keyId"), "DLT header 不得包含 keyId");
        assertFalse(headerKeys.contains("proof"), "DLT header 不得包含 proof");
        assertFalse(headerKeys.contains("certificateDigest"), "DLT header 不得包含证书摘要");

        List<String> headerValues = Arrays.stream(sent.headers().toArray())
                .map(header -> new String(header.value(), StandardCharsets.UTF_8))
                .toList();
        assertFalse(headerValues.contains("K-UP"), "DLT header 不得回显 keyId");
    }

    @Test
    void publish_WhenDltDisabled_ShouldFailWithoutSending() {
        properties.getDlt().setEnabled(false);

        assertThrows(TboxV2cKeyProvisionedDltException.class,
                () -> publisher.publish(record, "PERMANENT_DEVICE_STATE", 1, null, null).block());
    }
}
