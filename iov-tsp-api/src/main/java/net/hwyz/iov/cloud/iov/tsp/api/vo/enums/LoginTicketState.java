package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

/**
 * 一次性登录票据状态（IOV-TSP-DSN-CR-019 D019-01）。
 * <p>
 * 迁移路径：ISSUED → CONSUMED/REVOKED；ISSUED → EXPIRED（由消费判定即时拒绝，
 * 归档任务只做状态收敛，不参与正确性）。CONSUMED/REVOKED 为终态。
 *
 * @author hwyz_leo
 */
public enum LoginTicketState {

    /**
     * 已签发，等待消费（同一会话同一时刻至多一张）
     */
    ISSUED,

    /**
     * 已被消费（原子条件更新，仅一次成功）
     */
    CONSUMED,

    /**
     * 已作废（重签替换或登录成功回执联动）
     */
    REVOKED,

    /**
     * 已过期（服务端 Clock 判定）
     */
    EXPIRED
}
