package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

/**
 * 交付事实读取结论质量（VSO 防腐层归一化结果，不直接暴露外部枚举）。
 *
 * @author hwyz_leo
 */
public enum DeliveryFactsQuality {

    /**
     * 唯一可解释的账号—订单—VIN 关联
     */
    MATCHED,

    /**
     * 无匹配关联
     */
    NO_MATCH,

    /**
     * 多个候选订单
     */
    AMBIGUOUS,

    /**
     * 事实不足
     */
    INSUFFICIENT,

    /**
     * 事实冲突
     */
    INCONSISTENT
}
