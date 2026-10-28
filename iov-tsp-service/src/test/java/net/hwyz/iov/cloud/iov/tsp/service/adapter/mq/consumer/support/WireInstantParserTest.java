package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WireInstantParser 单元测试（TSP-DSN-CR-013）。
 * <p>
 * 锁定消息边界（hutool JSON 路径）双读契约：ISO 字符串 / 10 位 epoch 秒 / 13 位 epoch 毫秒
 * 归一至同一 Instant；窗口外、小数、缺失、错误类型 fail-closed，禁止 now() 兜底。
 * 规则镜像 FW-WEB-DSN-CR-001 RD-001-2/RD-001-3。
 */
class WireInstantParserTest {

    private static final Instant ISO_INSTANT = Instant.parse("2026-10-10T08:00:00Z");
    /** 2026-10-10T08:00:00Z 的 epoch 毫秒 */
    private static final long EPOCH_MILLIS = ISO_INSTANT.toEpochMilli();
    /** 2026-10-10T08:00:00Z 的 epoch 秒 */
    private static final long EPOCH_SECONDS = ISO_INSTANT.getEpochSecond();

    @Test
    void parse_IsoString_ShouldParse() {
        assertEquals(ISO_INSTANT, WireInstantParser.parse("2026-10-10T08:00:00Z"));
    }

    @Test
    void parse_13DigitMillis_ShouldParseSameInstant() {
        assertEquals(ISO_INSTANT, WireInstantParser.parse(EPOCH_MILLIS));
    }

    @Test
    void parse_10DigitSeconds_ShouldParseSameInstant() {
        assertEquals(ISO_INSTANT, WireInstantParser.parse(EPOCH_SECONDS));
    }

    @Test
    void parse_NumberAndIso_ShouldBeEquivalent() {
        // 同一时刻的数字与 ISO 必须得到同一领域时间（幂等摘要与业务结果一致）
        assertEquals(WireInstantParser.parse("2026-10-10T08:00:00Z"), WireInstantParser.parse(EPOCH_MILLIS));
        assertEquals(WireInstantParser.parse("2026-10-10T08:00:00Z"), WireInstantParser.parse(EPOCH_SECONDS));
    }

    @Test
    void parse_Missing_ShouldFailClosed() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> WireInstantParser.parse(null));
        assertTrue(e.getMessage().contains("缺失"));
    }

    @Test
    void parse_BlankString_ShouldFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> WireInstantParser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> WireInstantParser.parse("   "));
    }

    @Test
    void parse_IllegalIsoString_ShouldFailClosed() {
        // 无 offset 字符串、非 ISO 文本、数字字符串一律拒绝，不猜测
        assertThrows(IllegalArgumentException.class, () -> WireInstantParser.parse("2026-10-10T08:00:00"));
        assertThrows(IllegalArgumentException.class, () -> WireInstantParser.parse("not-a-time"));
        assertThrows(IllegalArgumentException.class, () -> WireInstantParser.parse(String.valueOf(EPOCH_MILLIS)));
    }

    @Test
    void parse_OutOfWindowNumbers_ShouldFailClosed() {
        // 0 / 负值 / 其他位数不做猜测
        assertThrows(IllegalArgumentException.class, () -> WireInstantParser.parse(0L));
        assertThrows(IllegalArgumentException.class, () -> WireInstantParser.parse(-1L));
        assertThrows(IllegalArgumentException.class, () -> WireInstantParser.parse(999_999_999L));
        assertThrows(IllegalArgumentException.class, () -> WireInstantParser.parse(10_000_000_000L)); // 11 位
        assertThrows(IllegalArgumentException.class, () -> WireInstantParser.parse(99_999_999_999L)); // 11 位
        assertThrows(IllegalArgumentException.class, () -> WireInstantParser.parse(10_000_000_000_000L)); // 14 位
    }

    @Test
    void parse_FractionalNumber_ShouldFailClosed() {
        // 秒小数/亚毫秒：拒绝，不做无依据截断（含整数值的 .0 小数 token）
        assertThrows(IllegalArgumentException.class, () -> WireInstantParser.parse(1793113054.5d));
        assertThrows(IllegalArgumentException.class, () -> WireInstantParser.parse((double) EPOCH_MILLIS));
        assertThrows(IllegalArgumentException.class, () -> WireInstantParser.parse(1793113054.5f));
    }

    @Test
    void parse_IntegralBigDecimal_ShouldParse() {
        // 整数值 BigDecimal（无小数部分）按窗口规则归一
        assertEquals(ISO_INSTANT, WireInstantParser.parse(BigDecimal.valueOf(EPOCH_MILLIS)));
    }

    @Test
    void parse_FractionalBigDecimal_ShouldFailClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> WireInstantParser.parse(new BigDecimal("1793113054.5")));
    }

    @Test
    void parse_UnsupportedType_ShouldFailClosed() {
        // 数组等错误类型拒绝，不悄悄忽略
        assertThrows(IllegalArgumentException.class, () -> WireInstantParser.parse(new Object[]{1L}));
        assertThrows(IllegalArgumentException.class, () -> WireInstantParser.parse(Boolean.TRUE));
    }
}
