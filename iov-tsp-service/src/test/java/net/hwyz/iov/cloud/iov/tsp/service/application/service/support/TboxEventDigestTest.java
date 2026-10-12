package net.hwyz.iov.cloud.iov.tsp.service.application.service.support;

import net.hwyz.iov.cloud.iov.tsp.service.application.dto.command.RecordTboxOnlineCmd;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TboxEventDigestTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-10-10T08:00:00Z");

    private RecordTboxOnlineCmd cmd(long generation) {
        return new RecordTboxOnlineCmd("EVT-1", "HSM-UID-0001", "VIN-1", "SN-1", "S-1",
                generation, OCCURRED_AT, "EMQX", "vagw.device.online");
    }

    @Test
    void deviceOnline_ShouldBeStableAndFieldSensitive() {
        String digest = TboxEventDigest.deviceOnline(cmd(1L));
        assertNotNull(digest);
        assertEquals(digest, TboxEventDigest.deviceOnline(cmd(1L)));
        assertNotEquals(digest, TboxEventDigest.deviceOnline(cmd(2L)));
    }

    @Test
    void deviceOnline_NullCmd_ShouldReturnNull() {
        assertNull(TboxEventDigest.deviceOnline(null));
    }
}
