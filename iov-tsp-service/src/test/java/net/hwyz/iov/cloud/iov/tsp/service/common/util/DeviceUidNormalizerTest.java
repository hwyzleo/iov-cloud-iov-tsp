package net.hwyz.iov.cloud.iov.tsp.service.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceUidNormalizerTest {

    @Test
    void normalize_ShouldTrimAndUppercase() {
        assertEquals("HSM-UID-0001", DeviceUidNormalizer.normalize("  hsm-uid-0001  "));
        assertNull(DeviceUidNormalizer.normalize(null));
    }

    @Test
    void normalize_ShouldStripHexPrefix() {
        assertEquals("A1B2C3", DeviceUidNormalizer.normalize("0xa1b2c3"));
        assertEquals("A1B2C3", DeviceUidNormalizer.normalize("0XA1B2C3"));
    }

    @Test
    void matches_ShouldCompareNormalizedValues() {
        assertTrue(DeviceUidNormalizer.matches("hsm-0001", "  HSM-0001 "));
        assertTrue(DeviceUidNormalizer.matches("0xa1b2", "A1B2"));
        assertFalse(DeviceUidNormalizer.matches("HSM-0001", "HSM-0002"));
        assertFalse(DeviceUidNormalizer.matches(null, "HSM-0001"));
        assertFalse(DeviceUidNormalizer.matches("HSM-0001", null));
        assertFalse(DeviceUidNormalizer.matches("HSM-0001", "   "));
    }

    @Test
    void isValid_ShouldAcceptAllowedCharacters() {
        assertTrue(DeviceUidNormalizer.isValid("HSM-UID-0001"));
        assertTrue(DeviceUidNormalizer.isValid("A1-B2"));
    }

    @Test
    void isValid_ShouldRejectIllegalValues() {
        assertFalse(DeviceUidNormalizer.isValid(null));
        assertFalse(DeviceUidNormalizer.isValid(""));
        assertFalse(DeviceUidNormalizer.isValid(" hsm "));
        assertFalse(DeviceUidNormalizer.isValid("HSM_UID"));
        assertFalse(DeviceUidNormalizer.isValid("HSM.UID"));
        assertFalse(DeviceUidNormalizer.isValid("A".repeat(65)));
    }
}
