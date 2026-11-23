package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.messaging.kafka;

import cn.hutool.json.JSONUtil;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.ActivationPayloadDigest;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationFact;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 首次激活候选事实 Kafka 生产适配测试（终审修复 B-4：事件 payload 携带 R-10 口径 payloadDigest）。
 * <p>
 * 断言口径：事件 JSON 含 {@code payloadDigest}，值等于
 * {@code sha256(vin|bindingOperationId|sourceFactRef|occurredAt)}，且与 {@code eventId} 无关；
 * Key 仍为 VIN、Topic 取配置，幂等判定未改变。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ActivationFactKafkaPublisher 测试（B-4 payloadDigest）")
class ActivationFactKafkaPublisherTest {

    private static final String VIN = "LSVAA1234567890AB";
    private static final String OP = "BOP20261122094501000123";
    private static final String REF = "vso:order:SO20261120001#delivery";
    private static final Instant OCCURRED_AT = Instant.parse("2026-11-22T09:45:01.234Z");
    private static final String TOPIC = "tsp.user-vehicle-activation-fact";

    @Mock
    private ReactiveKafkaProducerTemplate<String, String> template;

    private ActivationFactKafkaPublisher publisher;

    @BeforeEach
    void setUp() {
        when(template.send(anyString(), anyString(), anyString())).thenReturn(Mono.empty());
        publisher = new ActivationFactKafkaPublisher(template, new TspUserVehicleProperties());
    }

    private ActivationFact fact(String eventId) {
        return ActivationFact.builder()
                .eventId(eventId)
                .schemaVersion(1)
                .vin(VIN)
                .bindingOperationId(OP)
                .sourceFactRef(REF)
                .occurredAt(OCCURRED_AT)
                .payloadDigest(ActivationPayloadDigest.compute(VIN, OP, REF, OCCURRED_AT))
                .publishStatus("PENDING")
                .build();
    }

    private String capturePayload() {
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(template).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo(TOPIC);
        assertThat(keyCaptor.getValue()).isEqualTo(VIN);
        return payloadCaptor.getValue();
    }

    @Test
    @DisplayName("事件 JSON 含 payloadDigest（R-10 口径），Key=VIN、Topic 取配置")
    void publishIncludesPayloadDigest() {
        publisher.publish(fact("UVA-1"));

        Map<?, ?> payload = JSONUtil.parseObj(capturePayload());
        assertThat(payload.get("payloadDigest"))
                .isEqualTo("ae68270cc361d8a5454e75810d18a77b02c03bf5a84e6dadc59eecea894ba0ea");
        assertThat(payload.get("occurredAt")).isEqualTo(OCCURRED_AT.toString());
        assertThat(payload.get("eventId")).isEqualTo("UVA-1");
        assertThat(payload.get("vin")).isEqualTo(VIN);
        assertThat(payload.get("schemaVersion")).isEqualTo(1);
    }

    @Test
    @DisplayName("payloadDigest 与 eventId 无关：同一业务事实不同 eventId 摘要一致")
    void payloadDigestIndependentOfEventId() {
        publisher.publish(fact("UVA-1"));
        publisher.publish(fact("UVA-2"));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(template, org.mockito.Mockito.times(2)).send(anyString(), anyString(), payloadCaptor.capture());
        Object first = JSONUtil.parseObj(payloadCaptor.getAllValues().get(0)).get("payloadDigest");
        Object second = JSONUtil.parseObj(payloadCaptor.getAllValues().get(1)).get("payloadDigest");
        assertThat(first).isNotNull().isEqualTo(second);
    }

    @Test
    @DisplayName("投递未启用：拒绝投递且不产生任何发送（幂等/开关语义不变）")
    void publishRejectedWhenDisabled() {
        TspUserVehicleProperties properties = new TspUserVehicleProperties();
        properties.getActivationFact().setPublishEnabled(false);
        ActivationFactKafkaPublisher disabled = new ActivationFactKafkaPublisher(template, properties);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> disabled.publish(fact("UVA-3")))
                .isInstanceOf(IllegalStateException.class);
        org.mockito.Mockito.verifyNoInteractions(template);
    }
}
