package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ActivationSessionState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.IviLoginResultStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.LoginTicketState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ConsumeLoginTicketResult;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.LoginTicketVo;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.ActivationLoginTicketErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.UserVehicleBindingErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.DigestUtils;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationLoginTicket;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationSession;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.BindingOperation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleAuditRecord;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationLoginTicketRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationSessionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.BindingOperationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleAuditRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 一次性登录票据应用服务（IOV-TSP-DSN-CR-019 D019-02/D019-03/D019-04/D019-05）。
 * <p>
 * 票据是「绑定事实 → 登录授权」的唯一桥接凭据：TSP 不签发账号令牌，只签发一次性、
 * 短 TTL、绑定四要素的搬运凭证；原值只在签发响应出现一次，服务端仅保存 SHA-256 摘要，
 * 消费为数据库原子条件更新（不先查后改），历史/重复/过期一律 fail-closed。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivationLoginTicketAppService {

    private static final int TICKET_BYTES = 32;
    private static final long RATE_WINDOW_MILLIS = 60_000L;

    private final ActivationSessionRepository activationSessionRepository;
    private final BindingOperationRepository bindingOperationRepository;
    private final ActivationLoginTicketRepository activationLoginTicketRepository;
    private final UserVehicleAuditRepository userVehicleAuditRepository;
    private final TspUserVehicleProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 会话级签发窗口（仅内存判定，不作为票据事实权威）
     */
    private final ConcurrentHashMap<String, Deque<Long>> issueWindows = new ConcurrentHashMap<>();

    /**
     * 车端签发一次性登录票据（会话已绑定且登录未完成）
     *
     * @param vin       可信车端上下文 VIN
     * @param sessionId 会话ID
     * @return 票据原值、到期时刻、会话与关系标识
     */
    public LoginTicketVo issue(String vin, String sessionId) {
        Instant now = clock.instant();
        if (!properties.getLoginTicket().isEnabled()) {
            // 回滚开关：停止签发即下线能力，已签发未消费票据自然过期
            log.warn("登录票据签发已关闭: sessionId={}", sessionId);
            throw business(ActivationLoginTicketErrorCode.LOGIN_TICKET_UNAVAILABLE);
        }
        ActivationSession session = activationSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> business(UserVehicleBindingErrorCode.SESSION_INVALID));
        if (!vin.equals(session.getVin())) {
            // 会话必须属于可信车端上下文，拒绝跨车申请
            audit("TICKET_REJECTED", ticketRefForSession(sessionId), vin, null, null, "REJECTED",
                    "VEHICLE", "SESSION_NOT_OWNED", null, now);
            throw business(UserVehicleBindingErrorCode.SESSION_INVALID);
        }
        validateIssuable(session, now);

        int nextSeq = activationLoginTicketRepository.maxIssueSeqBySession(sessionId) + 1;
        if (nextSeq > properties.getLoginTicket().getPerSessionMaxIssue()) {
            audit("TICKET_REJECTED", ticketRefForSession(sessionId), vin, null, null, "REJECTED",
                    "VEHICLE", "SESSION_ISSUE_LIMIT", null, now);
            throw business(ActivationLoginTicketErrorCode.RATE_LIMITED);
        }
        acquireIssueQuota(sessionId, now);

        String ticket = newTicketValue();
        String ticketHash = DigestUtils.sha256Hex(ticket);
        String ticketId = newId("TKT");
        String relationId = resolveRelationId(session.getBindingOperationId());
        Instant expiresAt = now.plus(properties.getLoginTicket().getTtl());
        ActivationLoginTicket entity = ActivationLoginTicket.builder()
                .ticketId(ticketId)
                .ticketHash(ticketHash)
                .sessionId(sessionId)
                .bindingOperationId(session.getBindingOperationId())
                .vin(session.getVin())
                .accountId(session.getClaimedAccountId())
                .relationId(relationId)
                .state(LoginTicketState.ISSUED)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .issueSeq(nextSeq)
                .build();
        try {
            transactionTemplate.executeWithoutResult(status -> {
                // 同一会话同一时刻至多一张有效票据：先作废旧票再签发新票
                activationLoginTicketRepository.revokeIssuedBySession(sessionId, now);
                activationLoginTicketRepository.insert(entity);
                audit("TICKET_ISSUED", ticketId, session.getVin(), null, null, "ISSUED", "VEHICLE", null,
                        "session:" + sessionId, now);
            });
        } catch (RuntimeException e) {
            releaseIssueQuota(sessionId, now);
            throw e;
        }
        log.info("签发登录票据: ticketId={}, sessionId={}, vin={}, expiresAt={}",
                ticketId, sessionId, maskVin(vin), expiresAt);
        return LoginTicketVo.builder()
                .ticket(ticket)
                .expiresAt(expiresAt)
                .sessionId(sessionId)
                .relationId(relationId)
                .build();
    }

    /**
     * 内部服务域消费一次性登录票据（原子单次消费，无业务副作用）
     *
     * @param ticket        票据原值
     * @param callerService 调用方服务名（仅记录，不做准入判定）
     * @return 票据绑定的四要素与关系标识
     */
    public ConsumeLoginTicketResult consume(String ticket, String callerService) {
        Instant now = clock.instant();
        if (isBlank(ticket)) {
            throw business(UserVehicleBindingErrorCode.INVALID_REQUEST);
        }
        String ticketHash = DigestUtils.sha256Hex(ticket.trim());
        String consumedBy = isBlank(callerService)
                ? properties.getLoginTicket().getConsumer() : callerService.trim();
        ActivationLoginTicket current = activationLoginTicketRepository.findByTicketHash(ticketHash)
                .orElseThrow(() -> {
                    audit("TICKET_REJECTED", hashRef(ticketHash), null, null, null, "REJECTED",
                            "SERVICE", "NOT_FOUND", null, now);
                    return business(ActivationLoginTicketErrorCode.LOGIN_TICKET_UNAVAILABLE);
                });
        if (current.isExpired(now)) {
            audit("TICKET_REJECTED", current.getTicketId(), current.getVin(), null,
                    current.getState() == null ? null : current.getState().name(), "REJECTED",
                    "SERVICE", "EXPIRED", null, now);
            throw business(ActivationLoginTicketErrorCode.LOGIN_TICKET_EXPIRED);
        }
        if (current.getState() != LoginTicketState.ISSUED) {
            throw rejectionOfState(current, now);
        }
        // 消费与审计同一事务：审计写失败则回滚消费，避免“票据已消费但无审计”
        Boolean consumed = transactionTemplate.execute(status -> {
            if (activationLoginTicketRepository.consume(ticketHash, consumedBy, now) == 0) {
                return Boolean.FALSE;
            }
            audit("TICKET_CONSUMED", current.getTicketId(), current.getVin(), null, "ISSUED", "CONSUMED",
                    "SERVICE", null, "session:" + current.getSessionId(), now);
            return Boolean.TRUE;
        });
        if (!Boolean.TRUE.equals(consumed)) {
            // 并发下已被消费/作废，或恰好跨过到期点：以数据库现状判定，不复用旧快照
            ActivationLoginTicket latest = activationLoginTicketRepository.findByTicketHash(ticketHash)
                    .orElseThrow(() -> business(ActivationLoginTicketErrorCode.LOGIN_TICKET_UNAVAILABLE));
            if (latest.isExpired(now)) {
                audit("TICKET_REJECTED", latest.getTicketId(), latest.getVin(), null,
                        latest.getState() == null ? null : latest.getState().name(), "REJECTED",
                        "SERVICE", "EXPIRED", null, now);
                throw business(ActivationLoginTicketErrorCode.LOGIN_TICKET_EXPIRED);
            }
            throw rejectionOfState(latest, now);
        }
        log.info("消费登录票据: ticketId={}, sessionId={}, vin={}, consumedBy={}",
                current.getTicketId(), current.getSessionId(), maskVin(current.getVin()), consumedBy);
        return ConsumeLoginTicketResult.builder()
                .accountId(current.getAccountId())
                .vin(current.getVin())
                .sessionId(current.getSessionId())
                .bindingOperationId(current.getBindingOperationId())
                .relationId(current.getRelationId())
                .expiresAt(current.getExpiresAt())
                .build();
    }

    /**
     * 登录成功回执联动：同一事务内作废该会话全部未消费票据（D019-05）。
     * <p>
     * 由登录回执应用服务在已开启的事务中调用；本方法不再单独开启事务。
     *
     * @param sessionId 会话ID
     * @param vin       车辆 VIN
     * @param now       当前绝对时刻
     * @return 作废张数
     */
    public int revokeIssuedOnLoginSuccess(String sessionId, String vin, Instant now) {
        int revoked = activationLoginTicketRepository.revokeIssuedBySession(sessionId, now);
        if (revoked > 0) {
            audit("TICKET_REJECTED", ticketRefForSession(sessionId), vin, null, "ISSUED", "REVOKED",
                    "SYSTEM", "LOGIN_SUCCEEDED", "session:" + sessionId, now);
            log.info("登录成功后作废未消费票据: sessionId={}, count={}", sessionId, revoked);
        }
        return revoked;
    }

    // ================= 内部实现 =================

    private void validateIssuable(ActivationSession session, Instant now) {
        boolean issuable = session.getState() == ActivationSessionState.BOUND
                && !isBlank(session.getBindingOperationId())
                && session.getIviLoginState() != IviLoginResultStatus.SUCCEEDED
                && session.getResultExpiresAt() != null
                && !now.isAfter(session.getResultExpiresAt())
                && !session.isExpired(now);
        if (!issuable) {
            // 未绑定/绑定操作缺失/登录已完成/恢复窗口过期/会话过期取消：一律拒绝，不降级签发
            audit("TICKET_REJECTED", ticketRefForSession(session.getSessionId()), session.getVin(), null,
                    session.getState() == null ? null : session.getState().name(), "REJECTED", "VEHICLE",
                    "PRECONDITION_NOT_MET", null, now);
            throw business(ActivationLoginTicketErrorCode.LOGIN_TICKET_UNAVAILABLE);
        }
    }

    private BusinessException rejectionOfState(ActivationLoginTicket ticket, Instant now) {
        LoginTicketState state = ticket.getState();
        audit("TICKET_REJECTED", ticket.getTicketId(), ticket.getVin(), null,
                state == null ? null : state.name(), "REJECTED", "SERVICE",
                state == null ? "STATE_UNKNOWN" : state.name(), null, now);
        if (state == LoginTicketState.CONSUMED) {
            return business(ActivationLoginTicketErrorCode.LOGIN_TICKET_CONSUMED);
        }
        if (state == LoginTicketState.EXPIRED) {
            return business(ActivationLoginTicketErrorCode.LOGIN_TICKET_EXPIRED);
        }
        // REVOKED 与未知状态统一按不可用处理，不泄露票据生命周期细节
        return business(ActivationLoginTicketErrorCode.LOGIN_TICKET_UNAVAILABLE);
    }

    private String resolveRelationId(String bindingOperationId) {
        if (isBlank(bindingOperationId)) {
            return null;
        }
        return bindingOperationRepository.findByOperationId(bindingOperationId)
                .map(BindingOperation::getResultRelationId)
                .orElse(null);
    }

    private void acquireIssueQuota(String sessionId, Instant now) {
        int limit = properties.getLoginTicket().getPerSessionPerMinute();
        if (limit <= 0) {
            return;
        }
        long nowMillis = now.toEpochMilli();
        Deque<Long> timestamps = issueWindows.computeIfAbsent(sessionId, key -> new ConcurrentLinkedDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && nowMillis - timestamps.peekFirst() >= RATE_WINDOW_MILLIS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= limit) {
                audit("TICKET_REJECTED", ticketRefForSession(sessionId), null, null, null, "REJECTED",
                        "VEHICLE", "RATE_LIMITED", null, now);
                throw business(ActivationLoginTicketErrorCode.RATE_LIMITED);
            }
            timestamps.addLast(nowMillis);
        }
    }

    private void releaseIssueQuota(String sessionId, Instant now) {
        Deque<Long> timestamps = issueWindows.get(sessionId);
        if (timestamps == null) {
            return;
        }
        synchronized (timestamps) {
            timestamps.remove(now.toEpochMilli());
        }
    }

    private String newTicketValue() {
        byte[] bytes = new byte[TICKET_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void audit(String bizType, String bizId, String vin, String accountId, String fromState,
                       String toState, String operatorSource, String reason, String factRef, Instant now) {
        userVehicleAuditRepository.insert(UserVehicleAuditRecord.builder()
                .auditId(newId("AUD"))
                .bizType(bizType)
                .bizId(bizId)
                .vin(maskVin(vin))
                .accountId(accountId)
                .fromState(fromState)
                .toState(toState)
                .operatorSource(operatorSource)
                .reason(reason)
                .factRef(factRef)
                .occurredAt(now)
                .build());
    }

    private String ticketRefForSession(String sessionId) {
        return "session:" + (sessionId == null ? "unknown" : sessionId);
    }

    private String hashRef(String ticketHash) {
        return ticketHash == null ? "unknown" : ticketHash.substring(0, Math.min(16, ticketHash.length()));
    }

    private BusinessException business(net.hwyz.iov.cloud.framework.common.exception.ErrorCode errorCode) {
        return new BusinessException(errorCode);
    }

    private String newId(String prefix) {
        return prefix + IdUtil.fastSimpleUUID().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String maskVin(String vin) {
        if (vin == null || vin.length() <= 7) {
            return "***";
        }
        return vin.substring(0, 3) + "***" + vin.substring(vin.length() - 4);
    }
}
