package net.hwyz.iov.cloud.iov.tsp.service.common.util;

import java.time.Instant;

/**
 * 首次激活候选事实载荷摘要（R-10 定稿口径，与 EDD-VMD-DSN-CR-077 §4.4 的
 * {@code ActivationPayloadDigest} 逐字对齐）。
 * <p>
 * 定稿口径：{@code sha256(vin | bindingOperationId | sourceFactRef | occurredAt)}，Hex 小写。
 * <ul>
 *   <li><b>与 {@code eventId} 无关</b>：同一次业务事实因重投产生不同 {@code eventId} 时，摘要必须保持一致，
 *       因此摘要差异不构成冲突终态判定依据（R-10：同业务键不同摘要仍按 DUPLICATE 处理，保留首次）；</li>
 *   <li>{@code sourceFactRef} 为空时按空串参与（不写 {@code null} 字面量）；</li>
 *   <li>{@code occurredAt} 序列化为 ISO-8601（{@link Instant#toString()}，与 Kafka 事件 payload 的
 *       {@code occurredAt} 字段逐字一致，便于消费方（VMD）用同一字符串复算对拉）；</li>
 *   <li>旧口径 {@code sha256(eventId|vin|bindingOperationId)} 已废弃（终审修复 B-4），本类不实现、不依赖。</li>
 * </ul>
 * 摘要仅用于观测与数据质量核对，<b>不参与任何幂等 / 终态判定</b>（投递幂等键仍为 {@code eventId}）。
 *
 * @author hwyz_leo
 */
public final class ActivationPayloadDigest {

    private static final String SEPARATOR = "|";

    private ActivationPayloadDigest() {
    }

    /**
     * 按 R-10 定稿口径回算载荷摘要（与 VMD 侧 {@code ActivationPayloadDigest.compute} 完全同构）。
     *
     * @param vin                车架号（已规范化）
     * @param bindingOperationId 绑定操作 ID
     * @param sourceFactRef      来源事实引用（可空，空按空串参与）
     * @param occurredAt         事实发生时刻（UTC，序列化为 ISO-8601）
     * @return sha256 Hex 小写摘要
     */
    public static String compute(String vin, String bindingOperationId, String sourceFactRef, Instant occurredAt) {
        String payload = nullToEmpty(vin)
                + SEPARATOR + nullToEmpty(bindingOperationId)
                + SEPARATOR + nullToEmpty(sourceFactRef)
                + SEPARATOR + (occurredAt == null ? "" : occurredAt.toString());
        return DigestUtils.sha256Hex(payload);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
