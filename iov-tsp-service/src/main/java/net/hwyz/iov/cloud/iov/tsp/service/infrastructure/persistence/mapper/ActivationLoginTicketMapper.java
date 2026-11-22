package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.ActivationLoginTicketPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 一次性登录票据 DAO（IOV-TSP-DSN-CR-019）。
 * <p>
 * 语句与条件更新集中在 {@code mappers/ActivationLoginTicketMapper.xml}；
 * 消费以影响行数作为唯一性判定，不使用「先查后改」。
 *
 * @author hwyz_leo
 */
@Mapper
public interface ActivationLoginTicketMapper {

    /**
     * 新增票据
     *
     * @param po 票据
     * @return 影响行数
     */
    int insertPo(ActivationLoginTicketPo po);

    /**
     * 按摘要查询
     *
     * @param ticketHash 摘要
     * @return 票据
     */
    ActivationLoginTicketPo selectByTicketHash(@Param("ticketHash") String ticketHash);

    /**
     * 会话最大签发序号
     *
     * @param sessionId 会话ID
     * @return 最大序号（无记录返回 0）
     */
    int selectMaxIssueSeqBySession(@Param("sessionId") String sessionId);

    /**
     * 作废会话下全部未消费票据
     *
     * @param sessionId 会话ID
     * @param now       当前绝对时刻（UTC 墙钟）
     * @return 受影响行数
     */
    int revokeIssuedBySession(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);

    /**
     * 原子消费
     *
     * @param ticketHash 摘要
     * @param consumedBy 消费方
     * @param now        消费时刻（UTC 墙钟）
     * @return 受影响行数
     */
    int consume(@Param("ticketHash") String ticketHash, @Param("consumedBy") String consumedBy,
                @Param("now") LocalDateTime now);
}
