package net.hwyz.iov.cloud.iov.tsp.service.common.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 设备 UID（HSM UID / 证书 CN）统一规范化工具（TSP-DSN-CR-011 §3）
 * <p>
 * 设备导入、更新、准入查询、VIN→身份解析、DEVICE_ONLINE Consumer、以及本次
 * framework 返回的 {@code subjectCommonName} 与请求 uid 的比较，全部复用同一规范化口径：
 * {@code trim → 去 0x 前缀 → toUpperCase(Locale.ROOT)}，合法格式 {@code ^[A-Z0-9-]{1,64}$}。
 * <p>
 * 该口径与 VMD CSR 身份校验、VAGW 对端证书 CN 校验保持一致，避免同一设备身份在不同链路
 * 因规范化差异被误判为不一致。
 *
 * @author hwyz_leo
 */
public final class DeviceUidNormalizer {

    private static final int MAX_LENGTH = 64;
    private static final Pattern VALID_PATTERN = Pattern.compile("^[A-Z0-9-]{1,64}$");

    private DeviceUidNormalizer() {
    }

    /**
     * 规范化设备 UID：trim + 去 0x 前缀 + uppercase。
     *
     * @param value 原始值
     * @return 规范化值；输入为 null 时返回 null
     */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.regionMatches(true, 0, "0x", 0, 2)) {
            normalized = normalized.substring(2);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    /**
     * 比较两个设备 UID 规范化后是否一致；任一侧为 null/空返回 false。
     *
     * @param left  一侧设备 UID（如请求 uid）
     * @param right 另一侧设备 UID（如 framework 返回的 subjectCommonName）
     * @return 规范化后非空且相等返回 true
     */
    public static boolean matches(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return normalizedLeft != null && !normalizedLeft.isEmpty()
                && normalizedLeft.equals(normalizedRight);
    }

    /**
     * 校验规范化后的设备 UID 是否合法。
     *
     * @param normalized 规范化值
     * @return 合法返回 true
     */
    public static boolean isValid(String normalized) {
        return normalized != null
                && !normalized.isEmpty()
                && normalized.length() <= MAX_LENGTH
                && VALID_PATTERN.matcher(normalized).matches();
    }
}
