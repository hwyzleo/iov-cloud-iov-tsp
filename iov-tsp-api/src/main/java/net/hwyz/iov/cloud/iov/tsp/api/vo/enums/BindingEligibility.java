package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

/**
 * 绑车资格决策分类（IOV-TSP-DSN-CR-017 D017-03）。
 * <p>
 * 仅 ELIGIBLE 允许创建关系；UNDETERMINED/DEPENDENCY_UNAVAILABLE 一律不放行。
 *
 * @author hwyz_leo
 */
public enum BindingEligibility {

    /**
     * 资格通过
     */
    ELIGIBLE,

    /**
     * 资格明确不符
     */
    INELIGIBLE,

    /**
     * 无法确定（规则缺失/事实不足/歧义）
     */
    UNDETERMINED,

    /**
     * 依赖不可用（超时/故障），不 fallback 放行
     */
    DEPENDENCY_UNAVAILABLE
}
