package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

/**
 * 车端绑定结果中关系标识的来源（IOV-TSP-DSN-CR-020 D020-08，§14 裁决 R-6 新增）。
 * <p>
 * 车端据来源区分「本次绑定实际产生的关系」与「当前有效关系」，
 * <b>不得把 relationId 当作跨时间稳定主键</b>（关系变更会使当前有效关系漂移）。
 *
 * @author hwyz_leo
 */
public enum RelationSource {

    /**
     * 来源于本次会话关联绑定操作的 resultRelationId（本次绑定产生的稳定关系）
     */
    BINDING_OPERATION,

    /**
     * 回退为 VIN 当前有效关系（含最终仍为空的情形）
     */
    ACTIVE_RELATION
}
