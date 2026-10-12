package net.hwyz.iov.cloud.iov.tsp.service.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HsmUidNormalizerTest {

    @Test
    void normalize_ShouldTrimAndUppercase() {
        assertEquals("HSM-UID-0001", HsmUidNormalizer.normalize("  hsm-uid-0001  "));
        assertNull(HsmUidNormalizer.normalize(null));
    }

    @Test
    void isValid_ShouldAcceptAllowedCharacters() {
        assertTrue(HsmUidNormalizer.isValid("HSM-UID-0001"));
        assertTrue(HsmUidNormalizer.isValid("A1-B2"));
    }

    @Test
    void isValid_ShouldRejectIllegalValues() {
        assertFalse(HsmUidNormalizer.isValid(null));
        assertFalse(HsmUidNormalizer.isValid(""));
        assertFalse(HsmUidNormalizer.isValid(" hsm "));
        assertFalse(HsmUidNormalizer.isValid("HSM_UID"));
        assertFalse(HsmUidNormalizer.isValid("HSM.UID"));
        assertFalse(HsmUidNormalizer.isValid("A".repeat(65)));
    }
}
