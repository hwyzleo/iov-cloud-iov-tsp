package net.hwyz.iov.cloud.iov.tsp.service.common.util;

/**
 * 敏感设备标识脱敏工具
 * <p>
 * TSP-DSN-CR-009：
 * <ul>
 *   <li>HSM UID：默认前 4 后 4</li>
 *   <li>IMEI：默认前 4 后 4</li>
 *   <li>ICCID：默认前 6 后 4</li>
 * </ul>
 *
 * @author hwyz_leo
 */
public final class SensitiveDataMasker {

    private SensitiveDataMasker() {
    }

    /**
     * 保留前缀与后缀，中间以 * 掩码；长度不足以保留时整体掩码。
     *
     * @param value  原始值
     * @param prefix 保留前缀长度
     * @param suffix 保留后缀长度
     * @return 脱敏值
     */
    public static String mask(String value, int prefix, int suffix) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int length = value.length();
        if (length <= prefix + suffix) {
            return "*".repeat(length);
        }
        return value.substring(0, prefix)
                + "*".repeat(length - prefix - suffix)
                + value.substring(length - suffix);
    }

    public static String maskHsmUid(String value) {
        return mask(value, 4, 4);
    }

    public static String maskImei(String value) {
        return mask(value, 4, 4);
    }

    public static String maskIccid(String value) {
        return mask(value, 6, 4);
    }
}
