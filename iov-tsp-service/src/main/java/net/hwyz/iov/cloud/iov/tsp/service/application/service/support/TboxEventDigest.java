package net.hwyz.iov.cloud.iov.tsp.service.application.service.support;

import net.hwyz.iov.cloud.iov.tsp.api.vo.event.TboxV2cKeyProvisionedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.command.RecordTboxOnlineCmd;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * 注册激活事件契约不可逆摘要
 * <p>
 * TSP-DSN-CR-010：DEVICE_ONLINE 相似 eventId 内容冲突检测；V2C 事件沿用 CR-008 口径（不含 keyId）。
 * TSP-DSN-CR-012 §7：仅对规范化非敏感字段（deviceSn|businessDomain|purpose:version）计算，
 * 用于冲突识别，不保存 keyId 列表、proof、证书摘要或 Wrapped Key。
 *
 * @author hwyz_leo
 */
public final class TboxEventDigest {

    private TboxEventDigest() {
    }

    /**
     * DEVICE_ONLINE canonical digest 输入：
     * {@code hsmUid|vin|tboxSn|sessionId|connectionGeneration|occurredAt(epochMilli)|timeSource}，
     * 可选字段缺失以空串占位。
     */
    public static String deviceOnline(RecordTboxOnlineCmd cmd) {
        if (cmd == null) {
            return null;
        }
        String raw = String.join("|",
                nullSafe(cmd.hsmUid()),
                nullSafe(cmd.vin()),
                nullSafe(cmd.tboxSn()),
                nullSafe(cmd.sessionId()),
                Long.toString(cmd.connectionGeneration()),
                cmd.occurredAt() == null ? "" : Long.toString(cmd.occurredAt().toEpochMilli()),
                nullSafe(cmd.timeSource()));
        return sha256Hex(raw);
    }

    /**
     * V2C 聚合完成事件契约摘要（不含 keyId）：{@code deviceSn|domain|purpose:version(排序)}。
     */
    public static String v2c(TboxV2cKeyProvisionedEvent event) {
        if (event == null) {
            return null;
        }
        List<String> parts = event.getKeys() == null ? List.of() : event.getKeys().stream()
                .map(key -> nullSafe(key.getPurpose()) + ":" + (key.getBusinessKeyVersion() == null ? "" : key.getBusinessKeyVersion()))
                .sorted()
                .toList();
        String raw = nullSafe(event.getDeviceSn()) + "|" + nullSafe(event.getBusinessDomain()) + "|" + String.join(",", parts);
        return sha256Hex(raw);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
