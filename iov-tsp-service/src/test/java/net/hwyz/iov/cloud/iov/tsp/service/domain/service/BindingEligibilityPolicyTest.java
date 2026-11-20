package net.hwyz.iov.cloud.iov.tsp.service.domain.service;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingEligibility;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingResultReason;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.DeliveryFacts;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.DeliveryFactsQuality;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.EligibilityDecision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 绑车资格策略测试（IOV-TSP-DSN-CR-017 TC017-03）。
 */
class BindingEligibilityPolicyTest {

    private final BindingEligibilityPolicy policy = new BindingEligibilityPolicy();

    private DeliveryFacts facts(DeliveryFactsQuality quality, boolean delivered, List<String> roles) {
        return new DeliveryFacts(quality, delivered, roles, "ORDER-1", "order=ORDER-1", Instant.now());
    }

    @Test
    @DisplayName("已交付且存在账号角色关联：资格通过")
    void eligibleWhenDeliveredWithRoles() {
        EligibilityDecision decision = policy.decide(facts(DeliveryFactsQuality.MATCHED, true, List.of("order_user")));
        assertThat(decision.eligibility()).isEqualTo(BindingEligibility.ELIGIBLE);
        assertThat(decision.reason()).isNull();
        assertThat(decision.ruleVersion()).isEqualTo(BindingEligibilityPolicy.RULE_VERSION);
    }

    @Test
    @DisplayName("650/900 归一为已交付：仍只是必要条件，角色缺失时无法确定")
    void undeterminedWhenRolesMissing() {
        EligibilityDecision decision = policy.decide(facts(DeliveryFactsQuality.MATCHED, true, List.of()));
        assertThat(decision.eligibility()).isEqualTo(BindingEligibility.UNDETERMINED);
        assertThat(decision.reason()).isEqualTo(BindingResultReason.ELIGIBILITY_UNDETERMINED);
    }

    @Test
    @DisplayName("未交付：明确不符")
    void ineligibleWhenNotDelivered() {
        EligibilityDecision decision = policy.decide(facts(DeliveryFactsQuality.MATCHED, false, List.of("buyer")));
        assertThat(decision.eligibility()).isEqualTo(BindingEligibility.INELIGIBLE);
        assertThat(decision.reason()).isEqualTo(BindingResultReason.ELIGIBILITY_REJECTED);
    }

    @Test
    @DisplayName("无账号-车辆关联：明确不符")
    void ineligibleWhenNoMatch() {
        EligibilityDecision decision = policy.decide(facts(DeliveryFactsQuality.NO_MATCH, false, List.of()));
        assertThat(decision.eligibility()).isEqualTo(BindingEligibility.INELIGIBLE);
    }

    @Test
    @DisplayName("事实歧义/不足/冲突：无法确定，不自动通过")
    void undeterminedForAmbiguousInsufficientInconsistent() {
        assertThat(policy.decide(facts(DeliveryFactsQuality.AMBIGUOUS, true, List.of("buyer"))).eligibility())
                .isEqualTo(BindingEligibility.UNDETERMINED);
        assertThat(policy.decide(facts(DeliveryFactsQuality.INSUFFICIENT, true, List.of("buyer"))).eligibility())
                .isEqualTo(BindingEligibility.UNDETERMINED);
        assertThat(policy.decide(facts(DeliveryFactsQuality.INCONSISTENT, true, List.of("buyer"))).eligibility())
                .isEqualTo(BindingEligibility.UNDETERMINED);
    }

    @Test
    @DisplayName("事实缺失（null）：无法确定，不放行")
    void undeterminedWhenFactsNull() {
        assertThat(policy.decide(null).eligibility()).isEqualTo(BindingEligibility.UNDETERMINED);
    }
}
