package net.hwyz.iov.cloud.iov.tsp.service.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 激活事实载荷摘要测试（终审修复 B-4：R-10 定稿口径）。
 * <p>
 * 核心断言：
 * <ul>
 *   <li>口径 = {@code sha256(vin|bindingOperationId|sourceFactRef|occurredAt)}，Hex 小写 64 位；</li>
 *   <li><b>与 eventId 无关</b>（入参结构上不含 eventId；同业务事实稳定复算）；</li>
 *   <li>与 EDD-VMD 口径一致：用 VMD 侧 {@code ActivationPayloadDigestTest} 的<b>同一固定样例</b>，
 *       断言两侧必须产出同一个固定期望值（对拉验证，任一侧改口径即失败）；</li>
 *   <li>{@code sourceFactRef} 为空按空串参与（不写 {@code null} 字面量）。</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@DisplayName("ActivationPayloadDigest 测试（B-4 R-10 口径）")
class ActivationPayloadDigestTest {

    /** 与 EDD-VMD `ActivationPayloadDigestTest` 完全相同的固定样例（对拉基线） */
    private static final String VIN = "LSVAA1234567890AB";
    private static final String OP = "BOP20261122094501000123";
    private static final String REF = "vso:order:SO20261120001#delivery";
    private static final Instant OCCURRED_AT = Instant.parse("2026-11-22T09:45:01.234Z");

    /**
     * 固定样例的期望摘要（由 canonical 串手工复算得出，VMD 侧同口径必须一致）
     */
    private static final String EXPECTED_WITH_REF = "ae68270cc361d8a5454e75810d18a77b02c03bf5a84e6dadc59eecea894ba0ea";
    private static final String EXPECTED_NULL_REF = "98b91c057a367e0d51180dbe3e5af28fd40ecd38365fd315babcbbe3005f964d";

    @Test
    @DisplayName("定稿口径：sha256(vin|bindingOperationId|sourceFactRef|occurredAt)，固定样例对拉 VMD")
    void computeMatchesVmdFixedSample() {
        assertThat(ActivationPayloadDigest.compute(VIN, OP, REF, OCCURRED_AT)).isEqualTo(EXPECTED_WITH_REF);
        assertThat(ActivationPayloadDigest.compute(VIN, OP, null, OCCURRED_AT)).isEqualTo(EXPECTED_NULL_REF);
    }

    @Test
    @DisplayName("摘要与 eventId 无关：结构无 eventId 入参，同业务事实稳定复算")
    void digestIsIndependentOfEventId() {
        String first = ActivationPayloadDigest.compute(VIN, OP, REF, OCCURRED_AT);
        String second = ActivationPayloadDigest.compute(VIN, OP, REF, OCCURRED_AT);

        assertThat(first).isEqualTo(second).hasSize(64);
        assertThat(first.toLowerCase()).isEqualTo(first);
        // 旧口径 sha256(eventId|vin|bindingOperationId) 必须与新口径不同（口径已切换）
        String legacy = DigestUtils.sha256Hex("UVA20261122094501123456|" + VIN + "|" + OP);
        assertThat(first).isNotEqualTo(legacy);
    }

    @Test
    @DisplayName("sourceFactRef 为空按空串参与；其余业务字段变化改变摘要")
    void nullRefTreatedAsEmptyAndOtherFieldsAffectDigest() {
        assertThat(ActivationPayloadDigest.compute(VIN, OP, null, OCCURRED_AT))
                .isEqualTo(ActivationPayloadDigest.compute(VIN, OP, "", OCCURRED_AT));
        assertThat(ActivationPayloadDigest.compute(VIN, OP, null, OCCURRED_AT))
                .isNotEqualTo(ActivationPayloadDigest.compute("LSVAA1234567890ZZ", OP, null, OCCURRED_AT));
        assertThat(ActivationPayloadDigest.compute(VIN, OP, null, OCCURRED_AT))
                .isNotEqualTo(ActivationPayloadDigest.compute(VIN, OP, null,
                        Instant.parse("2026-11-22T09:45:02.000Z")));
    }

    @Test
    @DisplayName("occurredAt 空值按空串参与，不抛异常（不写 null 字面量）")
    void nullOccurredAtTreatedAsEmpty() {
        assertThat(ActivationPayloadDigest.compute(VIN, OP, REF, null))
                .isEqualTo(DigestUtils.sha256Hex(VIN + "|" + OP + "|" + REF + "|"));
    }
}
