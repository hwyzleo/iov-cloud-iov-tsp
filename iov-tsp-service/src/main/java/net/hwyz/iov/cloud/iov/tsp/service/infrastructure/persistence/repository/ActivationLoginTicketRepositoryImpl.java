package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.LoginTicketState;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.UtcTime;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationLoginTicket;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationLoginTicketRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.ActivationLoginTicketMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.ActivationLoginTicketPo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * 一次性登录票据仓储实现（时间列按 UTC 绝对时刻显式映射）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ActivationLoginTicketRepositoryImpl implements ActivationLoginTicketRepository {

    private final ActivationLoginTicketMapper activationLoginTicketMapper;

    @Override
    public void insert(ActivationLoginTicket ticket) {
        ActivationLoginTicketPo po = ActivationLoginTicketPo.builder()
                .ticketId(ticket.getTicketId())
                .ticketHash(ticket.getTicketHash())
                .sessionId(ticket.getSessionId())
                .bindingOperationId(ticket.getBindingOperationId())
                .vin(ticket.getVin())
                .accountId(ticket.getAccountId())
                .relationId(ticket.getRelationId())
                .state(ticket.getState() == null ? LoginTicketState.ISSUED.name() : ticket.getState().name())
                .issuedAt(UtcTime.toLocalDateTime(ticket.getIssuedAt()))
                .expiresAt(UtcTime.toLocalDateTime(ticket.getExpiresAt()))
                .consumedAt(UtcTime.toLocalDateTime(ticket.getConsumedAt()))
                .consumedBy(ticket.getConsumedBy())
                .issueSeq(ticket.getIssueSeq() == null ? 1 : ticket.getIssueSeq())
                .build();
        activationLoginTicketMapper.insertPo(po);
        ticket.setId(po.getId());
    }

    @Override
    public Optional<ActivationLoginTicket> findByTicketHash(String ticketHash) {
        return Optional.ofNullable(activationLoginTicketMapper.selectByTicketHash(ticketHash)).map(this::toEntity);
    }

    @Override
    public int maxIssueSeqBySession(String sessionId) {
        return activationLoginTicketMapper.selectMaxIssueSeqBySession(sessionId);
    }

    @Override
    public int revokeIssuedBySession(String sessionId, Instant now) {
        return activationLoginTicketMapper.revokeIssuedBySession(sessionId, UtcTime.toLocalDateTime(now));
    }

    @Override
    public int consume(String ticketHash, String consumedBy, Instant now) {
        return activationLoginTicketMapper.consume(ticketHash, consumedBy, UtcTime.toLocalDateTime(now));
    }

    private ActivationLoginTicket toEntity(ActivationLoginTicketPo po) {
        return ActivationLoginTicket.builder()
                .id(po.getId())
                .ticketId(po.getTicketId())
                .ticketHash(po.getTicketHash())
                .sessionId(po.getSessionId())
                .bindingOperationId(po.getBindingOperationId())
                .vin(po.getVin())
                .accountId(po.getAccountId())
                .relationId(po.getRelationId())
                .state(po.getState() == null ? null : LoginTicketState.valueOf(po.getState()))
                .issuedAt(UtcTime.toInstant(po.getIssuedAt()))
                .expiresAt(UtcTime.toInstant(po.getExpiresAt()))
                .consumedAt(UtcTime.toInstant(po.getConsumedAt()))
                .consumedBy(po.getConsumedBy())
                .issueSeq(po.getIssueSeq())
                .rowVersion(po.getRowVersion())
                .build();
    }
}
