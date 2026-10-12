package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model;

/**
 * 业务激活处理结果
 * <p>
 * TSP-DSN-CR-010 R4：领域服务只更新 {@code tb_tbox}，事件表写入由应用服务负责；
 * 领域服务通过本结果向应用层表达应落库的事件事实。
 *
 * @author hwyz_leo
 */
public enum ActivationOutcome {

    /**
     * ACTIVATING -> ACTIVE 条件更新成功
     */
    ACTIVATED,

    /**
     * 设备已为 ACTIVE，幂等成功
     */
    IDEMPOTENT_ACTIVE,

    /**
     * 设备仍为 PRE_ACTIVE（跨 Topic 乱序），进入重试/对账
     */
    OUT_OF_ORDER,

    /**
     * 契约/设备/绑定/状态校验不通过，fail-closed
     */
    REJECTED
}
