package net.hwyz.iov.cloud.iov.tsp.service.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 绝对事件时间显式 UTC 转换（TSP-DSN-CR-014）。
 * <p>
 * 所有「事件发生时刻（Instant）→ 数据库 DATETIME 墙钟」的转换必须显式使用 UTC，
 * 不得依赖 JVM 默认时区（{@code ZoneId.systemDefault()}）：同一事件在不同 JVM 时区下
 * 会写入不同墙钟，破坏跨环境一致性（RD-TIME-002 目标 Instant↔UTC DATETIME）。
 * <p>
 * 该工具仅在真实调用语义需要「绝对时刻 ↔ UTC 墙钟」时使用，不机械替换纯墙钟场景。
 *
 * @author hwyz_leo
 * @since 2026-10-31
 */
public final class UtcTime {

    public static final ZoneOffset UTC = ZoneOffset.UTC;

    private UtcTime() {
    }

    /**
     * 绝对时刻转 UTC 墙钟（用于 DATETIME 列写值）。
     *
     * @param instant 绝对时刻；null 返回 null
     * @return UTC 墙钟
     */
    public static LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, UTC) : null;
    }

    /**
     * UTC 墙钟转绝对时刻（用于 DATETIME 列读回值）。
     *
     * @param localDateTime UTC 墙钟；null 返回 null
     * @return 绝对时刻
     */
    public static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.toInstant(UTC) : null;
    }
}
