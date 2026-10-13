package net.hwyz.iov.cloud.iov.tsp.service.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 审计摘要工具（TSP-DSN-CR-011 §9）
 * <p>
 * 敏感标识（uid/certSerial/sourceIp）只允许以不可逆摘要落库与记录，禁止明文。
 *
 * @author hwyz_leo
 */
public final class DigestUtils {

    private DigestUtils() {
    }

    /**
     * 计算 SHA-256 十六进制摘要。
     *
     * @param value 原始值；为空时返回 null
     * @return 小写十六进制摘要
     */
    public static String sha256Hex(String value) {
        if (value == null) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
