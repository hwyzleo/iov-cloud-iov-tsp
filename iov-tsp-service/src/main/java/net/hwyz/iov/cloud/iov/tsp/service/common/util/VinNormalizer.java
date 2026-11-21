package net.hwyz.iov.cloud.iov.tsp.service.common.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * VIN 规范化与合法性校验（IOV-TSP-DSN-CR-018 D018-01）。
 * <p>
 * 口径与 VSO/VMD 及 framework 一致：{@code trim → toUpperCase(Locale.ROOT)} 后必须为
 * 17 位 {@code [A-HJ-NPR-Z0-9]}（ISO 3779，排除 I/O/Q）。查询入口在访问数据库前完成校验，
 * 非法 VIN 直接走参数错误契约，不得查询后返回 NOT_BOUND。
 *
 * @author hwyz_leo
 */
public final class VinNormalizer {

    /**
     * VIN 固定长度
     */
    private static final int VIN_LENGTH = 17;

    /**
     * VIN 合法字符集（ISO 3779）
     */
    private static final Pattern VIN_PATTERN = Pattern.compile("^[A-HJ-NPR-Z0-9]{" + VIN_LENGTH + "}$");

    private VinNormalizer() {
    }

    /**
     * 规范化 VIN：trim + uppercase。
     *
     * @param vin 原始 VIN
     * @return 规范化 VIN；输入为 null 时返回 null
     */
    public static String normalize(String vin) {
        if (vin == null) {
            return null;
        }
        return vin.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 判断规范化后的 VIN 是否为合法 17 位 VIN。
     *
     * @param vin 原始或已规范化 VIN
     * @return 是否合法
     */
    public static boolean isValid(String vin) {
        String normalized = normalize(vin);
        return normalized != null && VIN_PATTERN.matcher(normalized).matches();
    }
}
