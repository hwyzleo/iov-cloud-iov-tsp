package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ActivationSessionState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingCallerScope;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingEligibility;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingOperationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingResultReason;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.AgreementAcceptanceRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.ConfirmVehicleBindingRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ActivationResultVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.BindingOperationVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ConfirmVehicleBindingVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.CreateActivationSessionVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ScanActivationSessionVo;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.UserVehicleBindingErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.DigestUtils;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationFact;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationSession;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AgreementBundle;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AgreementContent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.BindingOperation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.DeliveryFacts;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.EligibilityDecision;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleAuditRecord;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleRelation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.VehicleFacts;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.AgreementContentPort;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.DependencyUnavailableException;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.OrderDeliveryFactsPort;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.VehicleFactsPort;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationFactRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationSessionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.BindingOperationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleAuditRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleRelationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.service.BindingEligibilityPolicy;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 用户车辆绑定应用服务（IOV-TSP-DSN-CR-017 D017-02/D017-03/D017-04/D017-05）。
 * <p>
 * 编排一次性会话、车辆/交付事实读取、资格决策、原子绑定与结果恢复；
 * 外部 RPC 一律在数据库事务与行锁之外执行，事务内按固定顺序锁定会话与幂等操作，
 * 数据库唯一键作为并发最终防线。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserVehicleBindingAppService {

    private static final int QR_TOKEN_BYTES = 32;

    private final AgreementContentPort agreementContentPort;
    private final VehicleFactsPort vehicleFactsPort;
    private final OrderDeliveryFactsPort orderDeliveryFactsPort;
    private final ActivationSessionRepository activationSessionRepository;
    private final UserVehicleRelationRepository userVehicleRelationRepository;
    private final BindingOperationRepository bindingOperationRepository;
    private final ActivationFactRepository activationFactRepository;
    private final UserVehicleAuditRepository userVehicleAuditRepository;
    private final BindingEligibilityPolicy bindingEligibilityPolicy;
    private final TspUserVehicleProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 车端查询激活协议（适用语言下的两项已发布协议）
     *
     * @param language 语言；null 使用默认语言
     * @return 协议发布包
     */
    public AgreementBundle queryAgreements(String language) {
        return loadAgreements(language == null ? properties.getDefaultLanguage() : language);
    }

    /**
     * 车端签发一次性二维码会话
     *
     * @param trustedVehicleRef 可信车端引用
     * @param vin               可信车端上下文 VIN
     * @param agreements        两项协议明确同意证据
     * @return 会话与二维码凭据
     */
    public CreateActivationSessionVo createSession(String trustedVehicleRef, String vin,
                                                   List<AgreementAcceptanceRequest> agreements) {
        AgreementBundle bundle = loadAgreements(properties.getDefaultLanguage());
        String revisions = validateAcceptances(agreements, bundle);
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.getSessionTtl());
        String qrToken = newQrToken();
        ActivationSession session = ActivationSession.builder()
                .sessionId(newId("SES"))
                .qrTokenHash(DigestUtils.sha256Hex(qrToken))
                .trustedVehicleRef(trustedVehicleRef)
                .vin(vin)
                .agreementRevisions(revisions)
                .expiresAt(expiresAt)
                .state(ActivationSessionState.ISSUED)
                .build();
        activationSessionRepository.insert(session);
        audit("SESSION", session.getSessionId(), vin, null, null, ActivationSessionState.ISSUED.name(),
                "VEHICLE", null, revisions, now);
        log.info("签发激活会话: sessionId={}, vin={}, expiresAt={}", session.getSessionId(), maskVin(vin), expiresAt);
        return CreateActivationSessionVo.builder()
                .sessionId(session.getSessionId())
                .qrToken(qrToken)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * App 扫码：认领会话并返回最小化待确认车辆信息（不代表绑定成功）
     *
     * @param accountId 可信账号
     * @param qrToken   二维码凭据
     * @return 最小车辆信息
     */
    public ScanActivationSessionVo scan(String accountId, String qrToken) {
        Instant now = clock.instant();
        String tokenHash = DigestUtils.sha256Hex(qrToken.trim());
        ActivationSession session = activationSessionRepository.findByQrTokenHash(tokenHash)
                .orElseThrow(() -> business(UserVehicleBindingErrorCode.SESSION_INVALID));
        if (session.getState() == ActivationSessionState.CANCELLED || session.getState() == ActivationSessionState.SKIPPED) {
            throw business(UserVehicleBindingErrorCode.SESSION_CANCELLED);
        }
        if (session.getState() == ActivationSessionState.BOUND) {
            // 已消费的二维码不可再用于绑车
            throw business(UserVehicleBindingErrorCode.SESSION_INVALID);
        }
        if (session.isExpired(now)) {
            throw business(UserVehicleBindingErrorCode.SESSION_EXPIRED);
        }
        if (session.getClaimedAccountId() != null && !session.getClaimedAccountId().equals(accountId)) {
            throw business(UserVehicleBindingErrorCode.SESSION_CLAIMED);
        }
        VehicleFacts vehicleFacts = loadVehicleFacts(session.getVin());
        if (!vehicleFacts.present()) {
            throw business(UserVehicleBindingErrorCode.VEHICLE_NOT_FOUND);
        }
        if (session.getClaimedAccountId() == null) {
            boolean claimed = activationSessionRepository.claim(session.getSessionId(), accountId, now,
                    session.getRowVersion() == null ? 1 : session.getRowVersion());
            if (!claimed) {
                ActivationSession latest = activationSessionRepository.findBySessionId(session.getSessionId())
                        .orElseThrow(() -> business(UserVehicleBindingErrorCode.SESSION_INVALID));
                if (latest.isExpired(now)) {
                    throw business(UserVehicleBindingErrorCode.SESSION_EXPIRED);
                }
                if (latest.getClaimedAccountId() != null && !latest.getClaimedAccountId().equals(accountId)) {
                    throw business(UserVehicleBindingErrorCode.SESSION_CLAIMED);
                }
                session = latest;
            } else {
                session.setClaimedAccountId(accountId);
                session.setClaimedAt(now);
                session.setState(ActivationSessionState.SCANNED);
            }
            audit("SESSION", session.getSessionId(), session.getVin(), accountId,
                    ActivationSessionState.ISSUED.name(), ActivationSessionState.SCANNED.name(),
                    "APP", null, null, now);
        }
        return ScanActivationSessionVo.builder()
                .sessionId(session.getSessionId())
                .vin(session.getVin())
                .state(session.getState())
                .expiresAt(session.getExpiresAt())
                .claimed(true)
                .build();
    }

    /**
     * App 取消会话（不修改已有云端关系）
     *
     * @param accountId 可信账号
     * @param sessionId 会话ID
     */
    public void cancel(String accountId, String sessionId) {
        Instant now = clock.instant();
        ActivationSession session = activationSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> business(UserVehicleBindingErrorCode.SESSION_INVALID));
        if (session.getClaimedAccountId() != null && !session.getClaimedAccountId().equals(accountId)) {
            throw business(UserVehicleBindingErrorCode.SESSION_CLAIMED);
        }
        if (session.getState() == ActivationSessionState.BOUND) {
            throw business(UserVehicleBindingErrorCode.SESSION_INVALID);
        }
        boolean cancelled = activationSessionRepository.cancel(sessionId, accountId, now,
                session.getRowVersion() == null ? 1 : session.getRowVersion());
        if (!cancelled) {
            throw business(UserVehicleBindingErrorCode.SESSION_CANCELLED);
        }
        audit("SESSION", sessionId, session.getVin(), accountId, session.getState().name(),
                ActivationSessionState.CANCELLED.name(), "APP", null, null, now);
    }

    /**
     * App 确认绑定（幂等 + 原子提交）
     *
     * @param accountId 可信账号
     * @param request   确认请求
     * @return 绑定结果
     */
    public ConfirmVehicleBindingVo confirm(String accountId, ConfirmVehicleBindingRequest request) {
        String sessionId = request.getSessionId().trim();
        String idempotencyKey = request.getIdempotencyKey().trim();
        String requestDigest = DigestUtils.sha256Hex(
                String.join("|", BindingCallerScope.APP.name(), accountId, sessionId));
        Instant now = clock.instant();

        Optional<BindingOperation> existing = bindingOperationRepository
                .findByIdempotencyKey(BindingCallerScope.APP, accountId, idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), requestDigest);
        }

        ActivationSession session = activationSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> business(UserVehicleBindingErrorCode.SESSION_INVALID));
        validateConfirmable(session, accountId, now);
        String vin = session.getVin();

        VehicleFacts vehicleFacts = loadVehicleFacts(vin);
        if (!vehicleFacts.present()) {
            throw business(UserVehicleBindingErrorCode.VEHICLE_NOT_FOUND);
        }
        DeliveryFacts deliveryFacts = loadDeliveryFacts(accountId, vin);
        EligibilityDecision decision = bindingEligibilityPolicy.decide(deliveryFacts);

        BindingOperation operation;
        try {
            operation = transactionTemplate.execute(status -> createPendingOperation(accountId, idempotencyKey,
                    requestDigest, sessionId, vin, decision));
        } catch (DuplicateKeyException e) {
            BindingOperation concurrent = bindingOperationRepository
                    .findByIdempotencyKey(BindingCallerScope.APP, accountId, idempotencyKey)
                    .orElseThrow(() -> business(UserVehicleBindingErrorCode.IDEMPOTENCY_CONFLICT));
            return replay(concurrent, requestDigest);
        }
        if (operation == null) {
            throw business(UserVehicleBindingErrorCode.INVALID_REQUEST);
        }

        if (!decision.eligible()) {
            commitFailure(operation, decision, deliveryFacts, now);
            throw reasonErrorCode(decision.reason());
        }
        try {
            return commitSuccess(operation, accountId, decision, deliveryFacts, now);
        } catch (RelationConflictSignal signal) {
            // 冲突必须在独立事务中落 FAILED（成功事务已整体回滚），保证同键重放结果一致
            commitFailure(operation, new EligibilityDecision(net.hwyz.iov.cloud.iov.tsp.api.vo.enums
                    .BindingEligibility.ELIGIBLE, BindingResultReason.RELATION_CONFLICT, decision.ruleVersion()),
                    deliveryFacts, now);
            throw business(UserVehicleBindingErrorCode.RELATION_CONFLICT);
        }
    }

    /**
     * 查询绑定操作结果（仅本人操作；查询不到不等于已失败）
     *
     * @param accountId   可信账号
     * @param operationId 操作ID
     * @return 操作结果
     */
    public BindingOperationVo queryOperation(String accountId, String operationId) {
        BindingOperation operation = bindingOperationRepository.findByOperationId(operationId)
                .orElseThrow(() -> business(UserVehicleBindingErrorCode.INVALID_REQUEST));
        if (!accountId.equals(operation.getAccountId())) {
            throw business(UserVehicleBindingErrorCode.INVALID_REQUEST);
        }
        return toOperationVo(operation);
    }

    /**
     * 车端查询本车绑定结果（独立恢复窗口内有效，不可再次用于绑车）
     *
     * @param vin       可信车端上下文 VIN
     * @param sessionId 会话ID
     * @return 绑定结果
     */
    public ActivationResultVo queryVehicleResult(String vin, String sessionId) {
        ActivationSession session = activationSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> business(UserVehicleBindingErrorCode.SESSION_INVALID));
        if (!vin.equals(session.getVin())) {
            throw business(UserVehicleBindingErrorCode.SESSION_INVALID);
        }
        Instant now = clock.instant();
        if (session.getResultExpiresAt() != null && now.isAfter(session.getResultExpiresAt())) {
            throw business(UserVehicleBindingErrorCode.SESSION_EXPIRED);
        }
        String relationId = userVehicleRelationRepository.findActiveByVin(vin)
                .map(UserVehicleRelation::getRelationId).orElse(null);
        return ActivationResultVo.builder()
                .sessionId(session.getSessionId())
                .state(session.getState())
                .bindingOperationId(session.getBindingOperationId())
                .relationId(relationId)
                .vin(vin)
                .iviLoginState(session.getIviLoginState())
                .resultExpiresAt(session.getResultExpiresAt())
                .build();
    }

    // ================= 内部实现 =================

    private AgreementBundle loadAgreements(String language) {
        try {
            return agreementContentPort.loadCurrent(language);
        } catch (DependencyUnavailableException e) {
            throw business(UserVehicleBindingErrorCode.DEPENDENCY_UNAVAILABLE);
        }
    }

    private String validateAcceptances(List<AgreementAcceptanceRequest> agreements, AgreementBundle bundle) {
        if (agreements == null || agreements.isEmpty()) {
            throw business(UserVehicleBindingErrorCode.AGREEMENT_NOT_ACCEPTED);
        }
        Set<String> acceptedTypes = new LinkedHashSet<>();
        List<String> revisions = new ArrayList<>();
        for (AgreementAcceptanceRequest acceptance : agreements) {
            if (acceptance == null || acceptance.getAgreementType() == null || !Boolean.TRUE.equals(acceptance.getAgreed())) {
                throw business(UserVehicleBindingErrorCode.AGREEMENT_NOT_ACCEPTED);
            }
            String type = acceptance.getAgreementType().name();
            AgreementContent published = bundle.find(type);
            if (published == null || !equalsText(published.revisionId(), acceptance.getRevisionId())) {
                // 拒绝与已发布修订不一致的同意证据（不得把可变 URL/最新版当作同意）
                throw business(UserVehicleBindingErrorCode.AGREEMENT_NOT_ACCEPTED);
            }
            if (!isBlank(acceptance.getContentHash()) && !isBlank(published.contentHash())
                    && !acceptance.getContentHash().trim().equals(published.contentHash().trim())) {
                // 客户端声明的内容摘要与 CMS 已发布内容不一致：拒绝，避免同意证据被替换
                throw business(UserVehicleBindingErrorCode.AGREEMENT_NOT_ACCEPTED);
            }
            acceptedTypes.add(type);
            revisions.add(type + ":" + acceptance.getRevisionId());
        }
        if (acceptedTypes.size() < 2) {
            throw business(UserVehicleBindingErrorCode.AGREEMENT_NOT_ACCEPTED);
        }
        return String.join(",", revisions);
    }

    private VehicleFacts loadVehicleFacts(String vin) {
        try {
            return vehicleFactsPort.load(vin);
        } catch (DependencyUnavailableException e) {
            throw business(UserVehicleBindingErrorCode.DEPENDENCY_UNAVAILABLE);
        }
    }

    private DeliveryFacts loadDeliveryFacts(String accountId, String vin) {
        try {
            return orderDeliveryFactsPort.load(accountId, vin);
        } catch (DependencyUnavailableException e) {
            // 瞬时失败不缓存为永久资格不符：操作保持 PENDING，允许同键重试
            log.warn("交付事实读取依赖不可用，保留可重试状态: vin={}", maskVin(vin));
            throw business(UserVehicleBindingErrorCode.DEPENDENCY_UNAVAILABLE);
        }
    }

    private void validateConfirmable(ActivationSession session, String accountId, Instant now) {
        if (session.getState() == ActivationSessionState.CANCELLED
                || session.getState() == ActivationSessionState.SKIPPED
                || session.getState() == ActivationSessionState.BOUND) {
            throw business(UserVehicleBindingErrorCode.SESSION_CANCELLED);
        }
        if (session.isExpired(now)) {
            throw business(UserVehicleBindingErrorCode.SESSION_EXPIRED);
        }
        if (session.getClaimedAccountId() != null && !session.getClaimedAccountId().equals(accountId)) {
            throw business(UserVehicleBindingErrorCode.SESSION_CLAIMED);
        }
        if (!session.confirmableBy(accountId, now)) {
            throw business(UserVehicleBindingErrorCode.SESSION_INVALID);
        }
    }

    private BindingOperation createPendingOperation(String accountId, String idempotencyKey, String requestDigest,
                                                     String sessionId, String vin, EligibilityDecision decision) {
        BindingOperation operation = BindingOperation.builder()
                .operationId(newId("BOP"))
                .callerScope(BindingCallerScope.APP)
                .accountId(accountId)
                .idempotencyKey(idempotencyKey)
                .requestDigest(requestDigest)
                .sessionId(sessionId)
                .vin(vin)
                .status(BindingOperationStatus.PENDING)
                .rowVersion(1)
                .eligibility(decision.eligibility())
                .ruleVersion(decision.ruleVersion())
                .build();
        bindingOperationRepository.insert(operation);
        return operation;
    }

    private ConfirmVehicleBindingVo commitSuccess(BindingOperation operation, String accountId,
                                                  EligibilityDecision decision, DeliveryFacts facts, Instant now) {
        return transactionTemplate.execute(status -> {
            BindingOperation locked = lockOperation(operation, accountId);
            if (locked.getStatus() == BindingOperationStatus.SUCCEEDED) {
                return toConfirmVo(locked);
            }
            if (locked.getStatus() == BindingOperationStatus.FAILED) {
                throw reasonErrorCode(locked.getResultReason());
            }
            ActivationSession session = activationSessionRepository.lockBySessionId(locked.getSessionId())
                    .orElseThrow(() -> business(UserVehicleBindingErrorCode.SESSION_INVALID));
            validateConfirmable(session, accountId, now);

            Optional<UserVehicleRelation> existingRelation =
                    userVehicleRelationRepository.findActiveByVin(locked.getVin());
            if (existingRelation.isPresent()) {
                UserVehicleRelation relation = existingRelation.get();
                if (!accountId.equals(relation.getAccountId())) {
                    throw new RelationConflictSignal();
                }
                // 同账号：幂等关联既有结果，不重复创建关系
                markSucceeded(locked, relation.getRelationId(), facts, now);
                audit("BINDING", locked.getOperationId(), locked.getVin(), accountId, "PENDING", "SUCCEEDED",
                        "APP", null, facts.factRef(), now);
                return toConfirmVo(locked);
            }

            if (!activationSessionRepository.markBound(session.getSessionId(), locked.getOperationId(), now,
                    now.plus(properties.getRecoveryWindow()), session.getRowVersion())) {
                throw business(UserVehicleBindingErrorCode.SESSION_INVALID);
            }

            UserVehicleRelation relation = UserVehicleRelation.builder()
                    .relationId(newId("REL"))
                    .accountId(accountId)
                    .vin(locked.getVin())
                    .role("PRIMARY_USER")
                    .state("ACTIVE")
                    .boundAt(now)
                    .sourceOperationId(locked.getOperationId())
                    .ruleVersion(decision.ruleVersion())
                    .factRef(facts.factRef())
                    .build();
            try {
                userVehicleRelationRepository.insert(relation);
            } catch (DuplicateKeyException e) {
                // active_vin 唯一键：并发下另一请求已建立关系，交由外层独立事务记录冲突
                throw new RelationConflictSignal();
            }

            markSucceeded(locked, relation.getRelationId(), facts, now);
            audit("BINDING", locked.getOperationId(), locked.getVin(), accountId, "PENDING", "SUCCEEDED",
                    "APP", null, facts.factRef(), now);
            saveActivationFact(locked, facts, now);
            return toConfirmVo(locked);
        });
    }

    private void commitFailure(BindingOperation operation, EligibilityDecision decision, DeliveryFacts facts,
                               Instant now) {
        transactionTemplate.executeWithoutResult(status -> {
            BindingOperation locked = lockOperation(operation, operation.getAccountId());
            if (locked.getStatus() != BindingOperationStatus.PENDING) {
                return;
            }
            markFailed(locked, decision.reason(), decision, facts, now);
            audit("BINDING", locked.getOperationId(), locked.getVin(), locked.getAccountId(), "PENDING", "FAILED",
                    "APP", decision.reason() == null ? null : decision.reason().name(), facts.factRef(), now);
        });
    }

    private BindingOperation lockOperation(BindingOperation operation, String accountId) {
        return bindingOperationRepository.lockByIdempotencyKey(BindingCallerScope.APP, accountId,
                operation.getIdempotencyKey())
                .orElseThrow(() -> business(UserVehicleBindingErrorCode.INVALID_REQUEST));
    }

    private void markSucceeded(BindingOperation operation, String relationId, DeliveryFacts facts, Instant now) {
        boolean updated = bindingOperationRepository.markSucceeded(operation.getOperationId(), relationId,
                facts.factRef(), facts.checkedAt(), now, operation.getRowVersion());
        if (!updated) {
            throw business(UserVehicleBindingErrorCode.SESSION_INVALID);
        }
        operation.setStatus(BindingOperationStatus.SUCCEEDED);
        operation.setResultRelationId(relationId);
        operation.setCompletedAt(now);
    }

    private void markFailed(BindingOperation operation, BindingResultReason reason, EligibilityDecision decision,
                            DeliveryFacts facts, Instant now) {
        boolean updated = bindingOperationRepository.markFailed(operation.getOperationId(), reason,
                decision.eligibility(), decision.ruleVersion(), facts.factRef(), facts.checkedAt(), now,
                operation.getRowVersion());
        if (!updated) {
            throw business(UserVehicleBindingErrorCode.SESSION_INVALID);
        }
        operation.setStatus(BindingOperationStatus.FAILED);
        operation.setResultReason(reason);
        operation.setCompletedAt(now);
    }

    private void saveActivationFact(BindingOperation operation, DeliveryFacts facts, Instant now) {
        if (!properties.getActivationFact().isPublishEnabled()) {
            return;
        }
        if (activationFactRepository.findByBusinessKey(operation.getVin(), operation.getOperationId()).isPresent()) {
            return;
        }
        String eventId = newId("UVA");
        ActivationFact fact = ActivationFact.builder()
                .eventId(eventId)
                .schemaVersion(properties.getActivationFact().getSchemaVersion())
                .vin(operation.getVin())
                .bindingOperationId(operation.getOperationId())
                .sourceFactRef(facts.factRef())
                .occurredAt(now)
                .payloadDigest(DigestUtils.sha256Hex(String.join("|", eventId, operation.getVin(),
                        operation.getOperationId())))
                .publishStatus("PENDING")
                .build();
        activationFactRepository.insert(fact);
        audit("ACTIVATION_FACT", eventId, operation.getVin(), operation.getAccountId(), null, "PENDING",
                "SYSTEM", null, facts.factRef(), now);
    }

    private ConfirmVehicleBindingVo replay(BindingOperation operation, String requestDigest) {
        if (!requestDigest.equals(operation.getRequestDigest())) {
            throw business(UserVehicleBindingErrorCode.IDEMPOTENCY_CONFLICT);
        }
        if (operation.getStatus() == BindingOperationStatus.FAILED) {
            throw reasonErrorCode(operation.getResultReason());
        }
        return toConfirmVo(operation);
    }

    private ConfirmVehicleBindingVo toConfirmVo(BindingOperation operation) {
        BindingResultReason reason = operation.getStatus() == BindingOperationStatus.PENDING
                ? BindingResultReason.PENDING : operation.getResultReason();
        return ConfirmVehicleBindingVo.builder()
                .operationId(operation.getOperationId())
                .status(operation.getStatus())
                .reason(reason)
                .relationId(operation.getResultRelationId())
                .vin(operation.getVin())
                .completedAt(operation.getCompletedAt())
                .build();
    }

    private BindingOperationVo toOperationVo(BindingOperation operation) {
        return BindingOperationVo.builder()
                .operationId(operation.getOperationId())
                .status(operation.getStatus())
                .reason(operation.getStatus() == BindingOperationStatus.PENDING
                        ? BindingResultReason.PENDING : operation.getResultReason())
                .relationId(operation.getResultRelationId())
                .vin(operation.getVin())
                .sessionId(operation.getSessionId())
                .completedAt(operation.getCompletedAt())
                .build();
    }

    private BusinessException business(UserVehicleBindingErrorCode errorCode) {
        return new BusinessException(errorCode);
    }

    private BusinessException reasonErrorCode(BindingResultReason reason) {
        if (reason == null) {
            return business(UserVehicleBindingErrorCode.ELIGIBILITY_UNDETERMINED);
        }
        return switch (reason) {
            case SESSION_INVALID -> business(UserVehicleBindingErrorCode.SESSION_INVALID);
            case SESSION_EXPIRED -> business(UserVehicleBindingErrorCode.SESSION_EXPIRED);
            case SESSION_CANCELLED -> business(UserVehicleBindingErrorCode.SESSION_CANCELLED);
            case SESSION_CLAIMED -> business(UserVehicleBindingErrorCode.SESSION_CLAIMED);
            case VEHICLE_NOT_FOUND -> business(UserVehicleBindingErrorCode.VEHICLE_NOT_FOUND);
            case ELIGIBILITY_REJECTED -> business(UserVehicleBindingErrorCode.ELIGIBILITY_REJECTED);
            case ELIGIBILITY_UNDETERMINED -> business(UserVehicleBindingErrorCode.ELIGIBILITY_UNDETERMINED);
            case DEPENDENCY_UNAVAILABLE -> business(UserVehicleBindingErrorCode.DEPENDENCY_UNAVAILABLE);
            case RELATION_CONFLICT -> business(UserVehicleBindingErrorCode.RELATION_CONFLICT);
            case IDEMPOTENCY_CONFLICT -> business(UserVehicleBindingErrorCode.IDEMPOTENCY_CONFLICT);
            case AGREEMENT_NOT_ACCEPTED -> business(UserVehicleBindingErrorCode.AGREEMENT_NOT_ACCEPTED);
            case REPORT_CONFLICT -> business(UserVehicleBindingErrorCode.REPORT_CONFLICT);
            case PENDING, INVALID_REQUEST -> business(UserVehicleBindingErrorCode.INVALID_REQUEST);
        };
    }

    private void audit(String bizType, String bizId, String vin, String accountId, String fromState,
                       String toState, String operatorSource, String reason, String factRef, Instant now) {
        userVehicleAuditRepository.insert(UserVehicleAuditRecord.builder()
                .auditId(newId("AUD"))
                .bizType(bizType)
                .bizId(bizId)
                .vin(vin)
                .accountId(accountId)
                .fromState(fromState)
                .toState(toState)
                .operatorSource(operatorSource)
                .reason(reason)
                .factRef(factRef)
                .occurredAt(now)
                .build());
    }

    private String newQrToken() {
        byte[] bytes = new byte[QR_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String newId(String prefix) {
        return prefix + IdUtil.fastSimpleUUID().toUpperCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean equalsText(String left, String right) {
        return left != null && left.equals(right);
    }

    /**
     * 关系冲突信号：在成功事务内抛出以回滚半完成状态，由外层独立事务记录 FAILED
     */
    private static class RelationConflictSignal extends RuntimeException {
        RelationConflictSignal() {
            super("relation conflict");
        }
    }

    private String maskVin(String vin) {
        if (vin == null || vin.length() <= 7) {
            return "***";
        }
        return vin.substring(0, 3) + "***" + vin.substring(vin.length() - 4);
    }
}
