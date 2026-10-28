package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * 消息边界 Instant 双读解析器（TSP-DSN-CR-013）。
 * <p>
 * hutool {@code JSONObject} 路径不经过 Jackson/Spring ObjectMapper（框架双读不自动生效），
 * 因此在消息边界显式适配：ISO-8601 字符串与旧整数时间戳均可解析，规则镜像
 * FW-WEB-DSN-CR-001 RD-001-2/RD-001-3（10 位整数 = epoch 秒，13 位整数 = epoch 毫秒，
 * 现代时间窗口 2001-09-09 ~ 2286；窗口外 0/负值/其他位数/小数/错误类型一律 fail-closed，
 * 不做无依据猜测，禁止用 now() 兜底）。
 * <p>
 * 同一时刻的数字与 ISO 输入必须得到同一 {@link Instant}（下游幂等摘要与业务结果一致）。
 *
 * @author hwyz_leo
 * @since 2026-10-28
 */
public final class WireInstantParser {

    /** 10 位 epoch 秒窗口下限：2001-09-09T01:46:40Z */
    private static final long SECONDS_MIN = 1_000_000_000L;
    /** 10 位 epoch 秒窗口上限：2286-11-20T17:46:39Z */
    private static final long SECONDS_MAX = 9_999_999_999L;
    /** 13 位 epoch 毫秒窗口下限：2001-09-09T01:46:40.000Z */
    private static final long MILLIS_MIN = 1_000_000_000_000L;
    /** 13 位 epoch 毫秒窗口上限：2286-11-20T17:46:39.999Z */
    private static final long MILLIS_MAX = 9_999_999_999_999L;

    private WireInstantParser() {
    }

    /**
     * 解析 wire 时间值。
     *
     * @param value hutool JSONObject 原始值（String / Number / null）
     * @return 归一后的 Instant
     * @throws IllegalArgumentException 缺失、空字符串、非法 ISO、窗口外数字、秒小数/亚毫秒、错误类型
     */
    public static Instant parse(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("occurredAt 缺失，禁止用当前时间兜底推进状态");
        }
        if (value instanceof String text) {
            if (text.isBlank()) {
                throw new IllegalArgumentException("occurredAt 为空字符串");
            }
            // ISO-8601 必须含 offset（Instant.parse 语义）；非法 fail-closed（统一包装为 IllegalArgumentException）
            try {
                return Instant.parse(text);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("occurredAt 非法 ISO-8601 时间（必须含 offset）: " + text, e);
            }
        }
        if (value instanceof Number number) {
            return fromNumber(number);
        }
        throw new IllegalArgumentException("occurredAt 类型不支持: " + value.getClass().getName());
    }

    private static Instant fromNumber(Number number) {
        if (number instanceof Double || number instanceof Float) {
            // 与 FW-WEB IovInstantDeserializer 一致：小数 token（含整数值的 .0）一律拒绝
            throw new IllegalArgumentException("occurredAt 拒绝小数时间戳（秒小数/亚毫秒）: " + number);
        }
        if (number instanceof BigDecimal decimal) {
            if (decimal.stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException("occurredAt 拒绝小数时间戳（秒小数/亚毫秒）: " + number);
            }
            return fromLong(decimal.longValueExact());
        }
        return fromLong(number.longValue());
    }

    private static Instant fromLong(long value) {
        if (value >= SECONDS_MIN && value <= SECONDS_MAX) {
            return Instant.ofEpochSecond(value);
        }
        if (value >= MILLIS_MIN && value <= MILLIS_MAX) {
            return Instant.ofEpochMilli(value);
        }
        throw new IllegalArgumentException(
                "occurredAt 不在 10 位 epoch 秒或 13 位 epoch 毫秒窗口内（0/负值/其他位数不做猜测）: " + value);
    }
}
