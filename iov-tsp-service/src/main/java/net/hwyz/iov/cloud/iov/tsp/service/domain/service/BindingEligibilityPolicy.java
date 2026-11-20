package net.hwyz.iov.cloud.iov.tsp.service.domain.service;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingEligibility;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingResultReason;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.DeliveryFacts;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.DeliveryFactsQuality;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.EligibilityDecision;
import org.springframework.stereotype.Component;

/**
 * 绑车资格策略（IOV-TSP-DSN-CR-017 D017-03）。
 * <p>
 * 只处理归一化事实与明确规则：
 * <ul>
 *   <li>650/900 由 VSO 归一为已交付，交付只是必要条件之一，不是充分资格；</li>
 *   <li>无匹配关联 → 明确不符；事实歧义/不足/冲突 → 无法确定；</li>
 *   <li>任何 UNKNOWN/DEPENDENCY 状态一律不自动通过。</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@Component
public class BindingEligibilityPolicy {

    /**
     * 本期资格规则版本（写入操作/关系证据，便于事后复核）
     */
    public static final String RULE_VERSION = "TSP-BIND-R1";

    /**
     * 归一化事实 → 资格决策
     *
     * @param facts 交付事实；null 视为无法确定
     * @return 资格决策
     */
    public EligibilityDecision decide(DeliveryFacts facts) {
        if (facts == null || facts.quality() == null) {
            return new EligibilityDecision(BindingEligibility.UNDETERMINED,
                    BindingResultReason.ELIGIBILITY_UNDETERMINED, RULE_VERSION);
        }
        if (facts.quality() == DeliveryFactsQuality.NO_MATCH) {
            return new EligibilityDecision(BindingEligibility.INELIGIBLE,
                    BindingResultReason.ELIGIBILITY_REJECTED, RULE_VERSION);
        }
        if (facts.quality() != DeliveryFactsQuality.MATCHED) {
            // AMBIGUOUS / INSUFFICIENT / INCONSISTENT：事实无法安全解释
            return new EligibilityDecision(BindingEligibility.UNDETERMINED,
                    BindingResultReason.ELIGIBILITY_UNDETERMINED, RULE_VERSION);
        }
        if (!facts.delivered()) {
            return new EligibilityDecision(BindingEligibility.INELIGIBLE,
                    BindingResultReason.ELIGIBILITY_REJECTED, RULE_VERSION);
        }
        if (!facts.rolesPresent()) {
            return new EligibilityDecision(BindingEligibility.UNDETERMINED,
                    BindingResultReason.ELIGIBILITY_UNDETERMINED, RULE_VERSION);
        }
        return new EligibilityDecision(BindingEligibility.ELIGIBLE, null, RULE_VERSION);
    }
}
