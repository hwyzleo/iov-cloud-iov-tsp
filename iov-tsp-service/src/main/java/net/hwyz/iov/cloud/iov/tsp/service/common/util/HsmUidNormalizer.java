package net.hwyz.iov.cloud.iov.tsp.service.common.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * HSM UID 规范化工具
 * <p>
 * TSP-DSN-CR-010：导入、更新、准入查询、VIN→身份解析与 DEVICE_ONLINE Consumer 复用同一规范化口径：
 * {@code trim().toUpperCase(Locale.ROOT)}，合法格式 {@code ^[A-Z0-9-]{1,64}$}。
 *
 * @author hwyz_leo
 */
public final class HsmUidNormalizer {

    private static final int MAX_LENGTH = 64;
    private static final Pattern VALID_PATTERN = Pattern.compile("^[A-Z0-9-]{1,64}$");

    private HsmUidNormalizer() {
    }

    /**
     * 规范化 HSM UID：trim + uppercase。
     *
     * @param value 原始值
     * @return 规范化值；输入为 null 时返回 null
     */
    public static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 校验规范化后的 HSM UID 是否合法。
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
