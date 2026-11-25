package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.FirstActivationState;

import java.time.Instant;

/**
 * 整车首次激活事实只读快照（IOV-TSP-DSN-CR-021 D021-02）。
 * <p>
 * 仅表达 VMD 官方只读结论；{@code UNKNOWN} 只用于查询失败/响应非法，
 * 不得与 {@code NOT_RECORDED} 混用，也不使用 TSP Outbox 投递状态代替。
 *
 * @param state            RECORDED/PENDING/NOT_RECORDED/UNKNOWN
 * @param firstActivatedAt 首次激活绝对时刻（仅 RECORDED 时有值）
 * @param factRef          事实摘要（不含 PII）
 * @author hwyz_leo
 */
public record FirstActivationFact(FirstActivationState state, Instant firstActivatedAt, String factRef) {

    /**
     * 查询不可用/响应非法
     *
     * @return UNKNOWN 快照
     */
    public static FirstActivationFact unknown() {
        return new FirstActivationFact(FirstActivationState.UNKNOWN, null, null);
    }
}
