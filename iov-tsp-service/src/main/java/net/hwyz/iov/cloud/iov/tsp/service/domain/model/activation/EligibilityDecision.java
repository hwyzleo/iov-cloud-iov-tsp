package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingEligibility;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingResultReason;

/**
 * 资格决策结果（分类 + 语义 reason + 规则版本）。
 *
 * @author hwyz_leo
 */
public record EligibilityDecision(BindingEligibility eligibility,
                                  BindingResultReason reason,
                                  String ruleVersion) {

    public boolean eligible() {
        return eligibility == BindingEligibility.ELIGIBLE;
    }
}
