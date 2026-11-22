package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationLoginTicket;

import java.time.Instant;
import java.util.Optional;

/**
 * 一次性登录票据仓储（IOV-TSP-DSN-CR-019 D019-01/D019-03）。
 * <p>
 * 所有状态迁移使用条件更新：消费为 {@code state='ISSUED' AND expires_at > now} 的原子
 * 条件更新，数据库影响行数即唯一性判定；不以「先查后改」代替。
 *
 * @author hwyz_leo
 */
public interface ActivationLoginTicketRepository {

    /**
     * 新增票据（仅保存摘要）
     *
     * @param ticket 票据
     */
    void insert(ActivationLoginTicket ticket);

    /**
     * 按票据摘要查询
     *
     * @param ticketHash 摘要
     * @return 票据
     */
    Optional<ActivationLoginTicket> findByTicketHash(String ticketHash);

    /**
     * 查询会话已签发序号（无记录返回 0）
     *
     * @param sessionId 会话ID
     * @return 最大签发序号
     */
    int maxIssueSeqBySession(String sessionId);

    /**
     * 作废会话下全部未消费票据（ISSUED → REVOKED）
     *
     * @param sessionId 会话ID
     * @param now       当前绝对时刻
     * @return 受影响行数
     */
    int revokeIssuedBySession(String sessionId, Instant now);

    /**
     * 原子消费（ISSUED → CONSUMED，且未过期）
     *
     * @param ticketHash 票据摘要
     * @param consumedBy 消费方标识
     * @param now        消费绝对时刻
     * @return 受影响行数（0 表示未消费成功，需按状态与到期判定具体错误）
     */
    int consume(String ticketHash, String consumedBy, Instant now);
}
