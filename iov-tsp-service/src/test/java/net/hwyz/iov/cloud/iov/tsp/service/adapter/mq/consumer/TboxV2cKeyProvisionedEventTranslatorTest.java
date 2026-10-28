package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.tsp.api.vo.event.TboxV2cKeyProvisionedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.TboxV2cKeyProvisionedContractException;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.JacksonInstantCompatModule;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspTboxV2cKeyProvisionedProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * V2C 聚合成功事件契约校验测试（TSP-DSN-CR-012 §4/§5/§10-3）
 */
class TboxV2cKeyProvisionedEventTranslatorTest {

    private static final String TOPIC = "vmd.tbox-v2c-key-provisioned";
    private static final String SN = "SN-0001";
    private static final Instant NOW = Instant.parse("2026-10-19T08:05:00Z");

    private TspTboxV2cKeyProvisionedProperties properties;
    private TboxV2cKeyProvisionedEventTranslator translator;

    @BeforeEach
    void setUp() {
        properties = new TspTboxV2cKeyProvisionedProperties();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        // 与生产环境一致：后注册兼容模块，数字按 epoch 毫秒解析（覆盖 JavaTimeModule 的 epoch 秒默认）
        objectMapper.registerModule(new JacksonInstantCompatModule());
        translator = new TboxV2cKeyProvisionedEventTranslator(objectMapper, properties);
    }

    private String payload() {
        return """
                {
                  "eventId": "EVT-1",
                  "requestId": "REQ-1",
                  "deviceSn": "SN-0001",
                  "businessDomain": "V2C_COMM",
                  "keys": [
                    {"purpose": "UPLINK_AEAD", "keyId": "K-UP", "businessKeyVersion": 1},
                    {"purpose": "DOWNLINK_AEAD", "keyId": "K-DOWN", "businessKeyVersion": 1}
                  ],
                  "result": "SUCCEEDED",
                  "occurredAt": "2026-10-19T08:00:00Z"
                }
                """;
    }

    private void assertCode(String expectedCode, String kafkaKey, String payload) {
        TboxV2cKeyProvisionedContractException exception = assertThrows(
                TboxV2cKeyProvisionedContractException.class,
                () -> translator.translate(TOPIC, kafkaKey, payload, NOW));
        assertEquals(expectedCode, exception.getErrorCode());
    }

    @Test
    void translate_ValidEvent_ShouldNormalizeContractVersion() {
        TboxV2cKeyProvisionedEvent event = translator.translate(TOPIC, SN, payload(), NOW);

        assertEquals(SN, event.getDeviceSn());
        assertEquals(TboxV2cKeyProvisionedEvent.CONTRACT_VERSION_V1, event.getContractVersion());
        assertEquals(2, event.getKeys().size());
    }

    @Test
    void translate_LegacyV1Alias_ShouldNormalizeToV1() {
        TboxV2cKeyProvisionedEvent event = translator.translate(TOPIC, SN,
                payload().replace("\"deviceSn\"", "\"contractVersion\":\"v1\",\"deviceSn\""), NOW);
        assertEquals(TboxV2cKeyProvisionedEvent.CONTRACT_VERSION_V1, event.getContractVersion());
    }

    @Test
    void translate_UnsupportedContractVersion_ShouldFailClosed() {
        assertCode("UNSUPPORTED_CONTRACT_VERSION", SN,
                payload().replace("\"deviceSn\"", "\"contractVersion\":\"2\",\"deviceSn\""));
    }

    @Test
    void translate_UnexpectedTopic_ShouldReject() {
        assertThrows(TboxV2cKeyProvisionedContractException.class,
                () -> translator.translate("other.topic", SN, payload(), NOW));
    }

    @Test
    void translate_EmptyPayload_ShouldReject() {
        assertCode("EMPTY_PAYLOAD", SN, " ");
    }

    @Test
    void translate_OversizedPayload_ShouldReject() {
        properties.setMaxPayloadBytes(16);
        assertCode("PAYLOAD_TOO_LARGE", SN, payload());
    }

    @Test
    void translate_MalformedJson_ShouldReject() {
        assertCode("MALFORMED_JSON", SN, "{not-json");
    }

    @Test
    void translate_KeyDeviceSnMismatch_ShouldReject() {
        assertCode("KEY_DEVICE_SN_MISMATCH", "SN-OTHER", payload());
    }

    @Test
    void translate_BlankKafkaKey_ShouldReject() {
        assertCode("KEY_DEVICE_SN_MISMATCH", null, payload());
    }

    @Test
    void translate_DeviceSnTooLong_ShouldReject() {
        String longSn = "S".repeat(65);
        assertCode("INVALID_DEVICE_SN", longSn, payload().replace(SN, longSn));
    }

    @Test
    void translate_EventIdTooLong_ShouldReject() {
        String longEventId = "E".repeat(65);
        assertCode("INVALID_EVENT_ID", SN, payload().replace("EVT-1", longEventId));
    }

    @Test
    void translate_RequestIdTooLong_ShouldReject() {
        String longRequestId = "R".repeat(129);
        assertCode("INVALID_REQUEST_ID", SN, payload().replace("REQ-1", longRequestId));
    }

    @Test
    void translate_StrictBusinessDomain_ShouldRejectTrailingSpace() {
        assertCode("INVALID_BUSINESS_DOMAIN", SN, payload().replace("V2C_COMM", "V2C_COMM "));
    }

    @Test
    void translate_StrictResult_ShouldRejectLowerCase() {
        assertCode("RESULT_NOT_SUCCEEDED", SN, payload().replace("SUCCEEDED", "succeeded"));
    }

    @Test
    void translate_SinglePurpose_ShouldReject() {
        String singlePurpose = payload().replace(
                ",\n    {\"purpose\": \"DOWNLINK_AEAD\", \"keyId\": \"K-DOWN\", \"businessKeyVersion\": 1}", "");
        assertCode("INVALID_KEY_SET", SN, singlePurpose);
    }

    @Test
    void translate_DuplicatePurpose_ShouldReject() {
        assertCode("DUPLICATE_PURPOSE", SN, payload().replace("DOWNLINK_AEAD", "UPLINK_AEAD"));
    }

    @Test
    void translate_BlankKeyId_ShouldReject() {
        assertCode("INVALID_KEY_ID", SN, payload().replace("\"K-UP\"", "\"\""));
    }

    @Test
    void translate_NonPositiveVersion_ShouldReject() {
        assertCode("INVALID_KEY_VERSION", SN,
                payload().replace("\"businessKeyVersion\": 1", "\"businessKeyVersion\": 0"));
    }

    @Test
    void translate_MissingOccurredAt_ShouldReject() {
        assertCode("MISSING_OCCURRED_AT", SN,
                payload().replace(",\n  \"occurredAt\": \"2026-10-19T08:00:00Z\"", ""));
    }

    @Test
    void translate_EpochMillisOccurredAt_ShouldParseLegacyVmdPayload() {
        // VMD 旧版生产者（hutool JSONUtil）将 occurredAt 序列化为 epoch 毫秒数字；
        // 加固后应按 epoch 毫秒解析而非 Jackson 默认的 epoch 秒（否则误读为公元 58791 年）。
        TboxV2cKeyProvisionedEvent event = translator.translate(TOPIC, SN,
                payload().replace("\"occurredAt\": \"2026-10-19T08:00:00Z\"", "\"occurredAt\": 1792396800000"), NOW);

        assertEquals(Instant.parse("2026-10-19T08:00:00Z"), event.getOccurredAt());
        assertEquals(TboxV2cKeyProvisionedEvent.CONTRACT_VERSION_V1, event.getContractVersion());
        assertEquals(2, event.getKeys().size());
    }

    @Test
    void translate_FutureOccurredAt_ShouldReject() {
        assertCode("OCCURRED_AT_FUTURE", SN, payload().replace("08:00:00Z", "09:00:00Z"));
    }
}
