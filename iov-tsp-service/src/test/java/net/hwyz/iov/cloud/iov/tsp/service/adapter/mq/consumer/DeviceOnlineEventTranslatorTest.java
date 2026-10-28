package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.framework.web.jackson.IovJavaTimeReadModule;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support.DeviceOnlineContractException;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.command.RecordTboxOnlineCmd;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeviceOnlineEventTranslatorTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-10-10T08:00:30Z");

    // TSP-DSN-CR-013：与生产装配一致（框架双读：ISO + 10 位秒/13 位毫秒）
    private final DeviceOnlineEventTranslator translator =
            new DeviceOnlineEventTranslator(new ObjectMapper().findAndRegisterModules()
                    .registerModule(new IovJavaTimeReadModule(ZoneId.of("Asia/Shanghai"))));

    private String payload(String overrides) {
        return "{\"eventId\":\"EVT-1\",\"eventType\":\"DEVICE_ONLINE\",\"hsmUid\":\"hsm-uid-0001\","
                + "\"vin\":\"VIN-1\",\"sessionId\":\"S-1\",\"connectionGeneration\":12,"
                + "\"occurredAt\":\"2026-10-10T08:00:00Z\",\"timeSource\":\"EMQX\"" + overrides + "}";
    }

    @Test
    void translate_ValidPayload_ShouldNormalizeHsmUid() {
        String json = payload(",\"tboxSn\":\"SN-1\"");
        RecordTboxOnlineCmd cmd = translator.translate("HSM-UID-0001", json, RECEIVED_AT, "vagw.device.online");

        assertEquals("HSM-UID-0001", cmd.hsmUid());
        assertEquals("EVT-1", cmd.eventId());
        assertEquals(12L, cmd.connectionGeneration());
        assertEquals(Instant.parse("2026-10-10T08:00:00Z"), cmd.occurredAt());
        assertEquals("vagw.device.online", cmd.sourceTopic());
    }

    @Test
    void translate_KeyHsmUidMismatch_ShouldReject() {
        assertThrows(DeviceOnlineContractException.class,
                () -> translator.translate("OTHER-KEY", payload(""), RECEIVED_AT, "t"));
    }

    @Test
    void translate_WrongEventType_ShouldReject() {
        String json = payload("").replace("DEVICE_ONLINE", "OTHER");
        assertThrows(DeviceOnlineContractException.class,
                () -> translator.translate("HSM-UID-0001", json, RECEIVED_AT, "t"));
    }

    @Test
    void translate_NonPositiveGeneration_ShouldReject() {
        String json = payload("").replace("\"connectionGeneration\":12", "\"connectionGeneration\":0");
        assertThrows(DeviceOnlineContractException.class,
                () -> translator.translate("HSM-UID-0001", json, RECEIVED_AT, "t"));
    }

    @Test
    void translate_MissingOccurredAt_ShouldReject() {
        String json = payload("").replace(",\"occurredAt\":\"2026-10-10T08:00:00Z\"", "");
        assertThrows(DeviceOnlineContractException.class,
                () -> translator.translate("HSM-UID-0001", json, RECEIVED_AT, "t"));
    }

    @Test
    void translate_EpochMillisOccurredAt_ShouldParseLegacyVagwPayload() {
        // 旧版 VAGW（hutool）epoch 毫秒 wire：与 ISO 同一时刻（TSP-DSN-CR-013 双读）
        String json = payload("").replace("\"occurredAt\":\"2026-10-10T08:00:00Z\"",
                "\"occurredAt\":" + Instant.parse("2026-10-10T08:00:00Z").toEpochMilli());
        RecordTboxOnlineCmd cmd = translator.translate("HSM-UID-0001", json, RECEIVED_AT, "vagw.device.online");
        assertEquals(Instant.parse("2026-10-10T08:00:00Z"), cmd.occurredAt());
    }

    @Test
    void translate_EpochSecondsOccurredAt_ShouldParse() {
        // 10 位 epoch 秒（现代窗口内按秒归一，RD-001-2）
        String json = payload("").replace("\"occurredAt\":\"2026-10-10T08:00:00Z\"",
                "\"occurredAt\":" + Instant.parse("2026-10-10T08:00:00Z").getEpochSecond());
        RecordTboxOnlineCmd cmd = translator.translate("HSM-UID-0001", json, RECEIVED_AT, "vagw.device.online");
        assertEquals(Instant.parse("2026-10-10T08:00:00Z"), cmd.occurredAt());
    }

    @Test
    void translate_FutureOccurredAt_ShouldReject() {
        String json = payload("").replace("2026-10-10T08:00:00Z", "2026-10-10T09:00:00Z");
        assertThrows(DeviceOnlineContractException.class,
                () -> translator.translate("HSM-UID-0001", json, RECEIVED_AT, "t"));
    }

    @Test
    void translate_InvalidTimeSource_ShouldReject() {
        String json = payload("").replace("EMQX", "CLIENT");
        assertThrows(DeviceOnlineContractException.class,
                () -> translator.translate("HSM-UID-0001", json, RECEIVED_AT, "t"));
    }

    @Test
    void translate_MalformedJson_ShouldReject() {
        assertThrows(DeviceOnlineContractException.class,
                () -> translator.translate("HSM-UID-0001", "{not-json", RECEIVED_AT, "t"));
    }
}
