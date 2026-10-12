package net.hwyz.iov.cloud.iov.tsp.service.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 敏感设备标识脱敏测试（TSP-DSN-CR-009）
 */
class SensitiveDataMaskerTest {

    @Test
    void maskHsmUid_ShouldKeepPrefix4Suffix4() {
        assertEquals("1234********CDEF", SensitiveDataMasker.maskHsmUid("1234567890ABCDEF"));
    }

    @Test
    void maskImei_ShouldKeepPrefix4Suffix4() {
        assertEquals("1234*******2345", SensitiveDataMasker.maskImei("123456789012345"));
    }

    @Test
    void maskIccid_ShouldKeepPrefix6Suffix4() {
        assertEquals("898600**********1234", SensitiveDataMasker.maskIccid("89860012345678901234"));
    }

    @Test
    void mask_ShouldMaskFullyWhenTooShort() {
        assertEquals("****", SensitiveDataMasker.maskHsmUid("1234"));
        assertEquals("******", SensitiveDataMasker.maskIccid("123456"));
    }

    @Test
    void mask_ShouldReturnNullOrEmptyUnchanged() {
        assertNull(SensitiveDataMasker.maskHsmUid(null));
        assertEquals("", SensitiveDataMasker.maskIccid(""));
    }
}
