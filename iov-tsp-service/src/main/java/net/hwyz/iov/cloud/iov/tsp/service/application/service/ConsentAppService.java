package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ActivationSessionState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.IviLoginResultStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.PrivacyAuthorizationState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.AgreementAcceptanceRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.ReportIviLoginResultRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.ReportPrivacyAuthorizationRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.SkipActivationRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ReportAckVo;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.UserVehicleBindingErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.DigestUtils;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AccountAgreementConsent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationSession;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AgreementConsent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.PrivacyAuthorization;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.PrivacyAuthorizationCatalog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleAuditRecord;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleRelation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationSessionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ConsentRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.PrivacyAuthorizationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleAuditRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleRelationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.service.PrivacyAuthorizationCatalogPolicy;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 协议同意与隐私授权应用服务（IOV-TSP-DSN-CR-017 D017-06）。
 * <p>
 * VIN 级跳过同意与 Account+VIN 级协议同意分表保存，禁止跨 scope 转写；
 * 补报按「可信来源 + reportId」幂等，同 ID 不同内容冲突；授权按来源版本收敛，
 * 不用接收时间覆盖新状态，也不把接收时间冒充用户动作时间。
 * <p>
 * IOV-TSP-DSN-CR-020：
 * <ul>
 *   <li>D020-03：隐私授权上报改为<b>两阶段</b>（先全量校验后写入），新增 401119 目录版本
 *       不一致与 401118 授权项不在批准目录内，两者均整批拒绝、零写入；items[].version
 *       语义为客户端决策序号，不再与目录版本比较；</li>
 *   <li>D020-06：登录回执 bindingOperationId 条件必填（SUCCEEDED 必填，FAILED 可省略），
 *       新增可选 failureReason 落审计；R-1 终态保护（迟到 FAILED 不回退 SUCCEEDED、
 *       不重开票据窗口）；R-2 审计仅首次写入时记录。</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentAppService {

    private static final String SOURCE_VEHICLE = "VEHICLE";

    private final ConsentRepository consentRepository;
    private final PrivacyAuthorizationRepository privacyAuthorizationRepository;
    private final ActivationSessionRepository activationSessionRepository;
    private final UserVehicleAuditRepository userVehicleAuditRepository;
    private final UserVehicleRelationRepository userVehicleRelationRepository;
    private final ActivationLoginTicketAppService activationLoginTicketAppService;
    private final PrivacyAuthorizationCatalog privacyAuthorizationCatalog;
    private final TspUserVehicleProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    /**
     * 车端跳过激活：幂等记录 VIN 级协议同意与跳过结果，不创建用户关系
     *
     * @param vin     可信车端上下文 VIN
     * @param request 跳过请求
     * @return 回执结果
     */
    public ReportAckVo skipActivation(String vin, SkipActivationRequest request) {
        validateAcceptances(request.getAgreements());
        Instant now = clock.instant();
        Instant reportedAgreedAt = parseOccurredAt(request.getOccurredAt());
        String payloadDigest = DigestUtils.sha256Hex(String.join("|", vin, request.getReportId(),
                revisionsOf(request.getAgreements())));

        for (AgreementAcceptanceRequest acceptance : request.getAgreements()) {
            String type = acceptance.getAgreementType().name();
            var existing = consentRepository.findVinConsent(vin, type, acceptance.getRevisionId(),
                    request.getReportId());
            if (existing.isPresent()) {
                if (!payloadDigest.equals(existing.get().getPayloadDigest())) {
                    throw business(UserVehicleBindingErrorCode.REPORT_CONFLICT);
                }
                continue;
            }
            consentRepository.insertVinConsent(AgreementConsent.builder()
                    .consentId(newId("CVS"))
                    .vin(vin)
                    .agreementType(type)
                    .version(acceptance.getVersion())
                    .revisionId(acceptance.getRevisionId())
                    .language(acceptance.getLanguage())
                    .contentHash(acceptance.getContentHash())
                    .source(SOURCE_VEHICLE)
                    .reportId(request.getReportId())
                    .payloadDigest(payloadDigest)
                    .reportedAgreedAt(reportedAgreedAt)
                    .receivedAt(now)
                    .build());
        }

        markSessionSkipped(vin, request.getSourceSessionId(), now);
        audit("CONSENT", request.getReportId(), vin, null, null, "VIN_CONSENT", SOURCE_VEHICLE, null, now);
        log.info("记录 VIN 级跳过同意: vin={}, reportId={}", maskVin(vin), request.getReportId());
        return ReportAckVo.builder().reportId(request.getReportId()).accepted(true).build();
    }

    /**
     * 可信车机登录结果回报：登录成功时按 Account+VIN 记录两项协议同意。
     * <p>
     * IOV-TSP-DSN-CR-020 D020-06 校验顺序：
     * <ol>
     *   <li>会话不存在/VIN 不归属/状态非 BOUND → 401101（BOUND 前置保留）；</li>
     *   <li>claimedAccountId 为空 → 401112；</li>
     *   <li>SUCCEEDED：bindingOperationId 必填且必须与会话一致 → 否则 401112（绝不降级为成功）；</li>
     *   <li>FAILED：bindingOperationId 可选，缺号仍记录失败事实；提供且不一致仍记录（审计标注 mismatch）；</li>
     *   <li>R-1 终态保护：UPDATE 影响 0 行（重复回执/会话已 SUCCEEDED）→ 视为已终态，
     *       不回退、不写同意、不重开票据窗口；R-2：审计仅首次写入时记录。</li>
     * </ol>
     *
     * @param vin     可信车端上下文 VIN
     * @param request 回执
     * @return 回执结果
     */
    public ReportAckVo reportIviLoginResult(String vin, ReportIviLoginResultRequest request) {
        ActivationSession session = activationSessionRepository.findBySessionId(request.getSessionId())
                .orElseThrow(() -> business(UserVehicleBindingErrorCode.SESSION_INVALID));
        if (!vin.equals(session.getVin())
                || session.getState() != ActivationSessionState.BOUND
                || session.getBindingOperationId() == null) {
            throw business(UserVehicleBindingErrorCode.SESSION_INVALID);
        }
        String accountId = session.getClaimedAccountId();
        if (accountId == null) {
            throw business(UserVehicleBindingErrorCode.INVALID_REQUEST);
        }
        boolean succeeded = request.getLoginStatus() == IviLoginResultStatus.SUCCEEDED;
        String reportedOperationId = isBlank(request.getBindingOperationId())
                ? null : request.getBindingOperationId().trim();
        if (succeeded && (reportedOperationId == null
                || !reportedOperationId.equals(session.getBindingOperationId()))) {
            // SUCCEEDED 的绑定操作号必填且必须与会话一致；不因缺失而降级为成功
            throw business(UserVehicleBindingErrorCode.INVALID_REQUEST);
        }
        // E2 定稿：FAILED 且绑定操作号与会话不一致时仍记录失败事实，仅审计标注，不返回 4xx
        boolean bindingOperationMismatch = reportedOperationId != null
                && !reportedOperationId.equals(session.getBindingOperationId());
        Instant now = clock.instant();
        // D019-05：登录成功写入与未消费票据作废必须在同一事务，避免出现“已登录仍可换取票据”的窗口
        ReportAckVo ack = transactionTemplate.execute(status -> {
            boolean firstWrite = false;
            if (!request.getReportId().equals(session.getIviLoginReportId())) {
                firstWrite = activationSessionRepository.reportIviLoginResult(session.getSessionId(),
                        request.getLoginStatus(), request.getReportId(), now);
            }
            if (!firstWrite) {
                // R-1/R-2：重复回执或会话已 SUCCEEDED 终态 → 不回退状态、不写同意、不重开票据窗口、不写审计
                log.info("车机登录回执重复或会话已终态，不重复写入: sessionId={}, reportId={}",
                        session.getSessionId(), request.getReportId());
                return ReportAckVo.builder().reportId(request.getReportId()).accepted(true).build();
            }
            if (succeeded) {
                saveAccountConsents(session, accountId, request, now);
                activationLoginTicketAppService.revokeIssuedOnLoginSuccess(session.getSessionId(), vin, now);
            }
            audit("LOGIN_RECEIPT", request.getReportId(), vin, accountId, null, request.getLoginStatus().name(),
                    SOURCE_VEHICLE, loginAuditReason(request, bindingOperationMismatch), now);
            return ReportAckVo.builder().reportId(request.getReportId()).accepted(true).build();
        });
        if (ack == null) {
            throw business(UserVehicleBindingErrorCode.INVALID_REQUEST);
        }
        return ack;
    }

    /**
     * 补报隐私授权（Account + VIN + 授权项，两阶段校验 + 同项单调收敛）。
     * <p>
     * IOV-TSP-DSN-CR-020 D020-03（G-1 定稿）：
     * <pre>
     * 阶段一（纯校验，无任何写入；目录强校验生效时顺序固定 401112 → 401119 → 401118）
     *   1. 逐项基础校验（编码/状态/版本非空且版本 >= 1）→ 401112；
     *      目录强校验生效时 catalogVersion 必填（缺失 → 401112）
     *   2. catalogVersion 必须等于服务端当前目录版本 → 否则 401119
     *   3. 每一项授权项必须命中当前目录 → 否则 401118
     * 阶段二（写入）同回执ID不同摘要 → 401113；同项收敛 >/=/< ；新增唯一键冲突 → 401114
     * </pre>
     * 阶段一完全无写入：第 N 项非法时不会出现“前 N-1 项已落库”的部分写入。
     * <p>
     * 终审修复 B-1（车端存量报文兼容）：{@code catalogVersion} 仅在目录强校验生效
     * （{@code enabled=true} 且 {@code enforce-catalog=true}）时必填；{@code enforce-catalog=false}
     * 时允许缺失并回退旧行为（不校验目录版本、不校验目录外授权项），日志/审计标记 {@code COMPAT_MODE}。
     *
     * @param accountId 可信账号
     * @param vin       可信车端上下文 VIN
     * @param request   授权回执
     * @return 回执结果
     */
    public ReportAckVo reportPrivacyAuthorization(String accountId, String vin,
                                                  ReportPrivacyAuthorizationRequest request) {
        Instant now = clock.instant();
        Instant reportedAt = parseOccurredAt(request.getOccurredAt());
        validatePrivacyReport(request);
        // IOV-TSP-DSN-CR-021 D021-08：授权必须归属当前 Account+VIN ACTIVE 关系周期；
        // 无当前关系或关系不匹配时不接受授权生效，也不把旧周期授权继承给新关系。
        String relationId = resolveActiveRelationId(accountId, vin);
        for (ReportPrivacyAuthorizationRequest.AuthorizationItem item : request.getItems()) {
            String authorizationItem = item.getAuthorizationItem().trim();
            String payloadDigest = DigestUtils.sha256Hex(String.join("|", accountId, vin, authorizationItem,
                    item.getState().name(), String.valueOf(item.getVersion())));
            var existing = privacyAuthorizationRepository.find(accountId, vin, relationId, authorizationItem);
            if (existing.isPresent()) {
                PrivacyAuthorization current = existing.get();
                if (request.getReportId().equals(current.getReportId())
                        && !payloadDigest.equals(current.getPayloadDigest())) {
                    throw business(UserVehicleBindingErrorCode.REPORT_CONFLICT);
                }
                long currentVersion = current.getVersion() == null ? 0L : current.getVersion();
                if (item.getVersion() <= currentVersion) {
                    // G-1 规则 3：等于 → 幂等跳过；小于 → 忽略且不报错（旧决策序号不得覆盖新状态）
                    continue;
                }
                PrivacyAuthorization update = PrivacyAuthorization.builder()
                        .authorizationId(current.getAuthorizationId())
                        .accountId(accountId)
                        .vin(vin)
                        .relationId(relationId)
                        .authorizationItem(authorizationItem)
                        .state(item.getState())
                        .version(item.getVersion())
                        .source(SOURCE_VEHICLE)
                        .reportId(request.getReportId())
                        .payloadDigest(payloadDigest)
                        .reportedAt(reportedAt)
                        .receivedAt(now)
                        .build();
                privacyAuthorizationRepository.updateIfNewer(update);
                continue;
            }
            try {
                privacyAuthorizationRepository.insert(PrivacyAuthorization.builder()
                        .authorizationId(newId("PAZ"))
                        .accountId(accountId)
                        .vin(vin)
                        .relationId(relationId)
                        .authorizationItem(authorizationItem)
                        .state(item.getState())
                        .version(item.getVersion())
                        .source(SOURCE_VEHICLE)
                        .reportId(request.getReportId())
                        .payloadDigest(payloadDigest)
                        .reportedAt(reportedAt)
                        .receivedAt(now)
                        .build());
            } catch (DuplicateKeyException e) {
                throw business(UserVehicleBindingErrorCode.AUTHORIZATION_VERSION_CONFLICT);
            }
        }
        audit("PRIVACY", request.getReportId(), vin, accountId, null,
                isCatalogEnforced() ? "PRIVACY_REPORT" : "PRIVACY_REPORT/COMPAT_MODE",
                SOURCE_VEHICLE, null, now);
        return ReportAckVo.builder().reportId(request.getReportId()).accepted(true).build();
    }

    /**
     * 解析当前 Account+VIN ACTIVE 关系周期ID（IOV-TSP-DSN-CR-021 D021-08）。
     * <p>
     * 无 ACTIVE 关系时拒绝授权生效（复用 401112：缺少可生效的关系上下文），
     * 不新建关系、不继承旧周期授权。
     *
     * @param accountId 可信账号
     * @param vin       可信车端上下文 VIN
     * @return 当前关系ID
     */
    private String resolveActiveRelationId(String accountId, String vin) {
        UserVehicleRelation relation = userVehicleRelationRepository.findActiveByAccountAndVin(accountId, vin)
                .orElseThrow(() -> business(UserVehicleBindingErrorCode.INVALID_REQUEST));
        return relation.getRelationId();
    }

    // ================= 内部实现 =================

    /**
     * 隐私授权上报阶段一校验（IOV-TSP-DSN-CR-020 D020-03，无任何写入）。
     * <p>
     * 目录强校验生效（{@code enabled=true} 且 {@code enforce-catalog=true}）时，校验顺序固定
     * 401112（字段/格式，含 catalogVersion 缺失）→ 401119（目录版本不等）→ 401118（目录外授权项），
     * 均为整批拒绝、零写入。
     * <p>
     * 终审修复 B-1：{@code enforce-catalog=false}（或目录能力关闭）时回退旧行为 ——
     * {@code catalogVersion} 允许缺失，不校验目录版本、不校验目录外授权项，仅日志标记
     * {@code COMPAT_MODE}（审计 reason 同步标记）。
     *
     * @param request 授权回执
     */
    private void validatePrivacyReport(ReportPrivacyAuthorizationRequest request) {
        // 规则 0：逐项基础校验（与是否强校验无关，始终生效）→ 401112
        for (ReportPrivacyAuthorizationRequest.AuthorizationItem item : request.getItems()) {
            if (item == null || isBlank(item.getAuthorizationItem()) || item.getState() == null
                    || item.getVersion() == null || item.getVersion() < 1) {
                throw business(UserVehicleBindingErrorCode.INVALID_REQUEST);
            }
            if (item.getState() == PrivacyAuthorizationState.INVALIDATED) {
                // INVALIDATED 是历史周期迁移/失效的本端状态，不接受车端作为授权结论上报
                throw business(UserVehicleBindingErrorCode.INVALID_REQUEST);
            }
        }
        boolean enforced = isCatalogEnforced();
        boolean versionReported = !isBlank(request.getCatalogVersion());
        if (enforced && !versionReported) {
            // 强校验生效时 catalogVersion 必填（缺失视为字段缺失 → 401112；顺序仍在 401119 之前）
            throw business(UserVehicleBindingErrorCode.INVALID_REQUEST);
        }
        if (privacyAuthorizationCatalog == null) {
            return;
        }
        String reportedCatalogVersion = versionReported ? request.getCatalogVersion().trim() : null;
        List<String> codes = request.getItems().stream()
                .map(item -> item.getAuthorizationItem().trim()).toList();
        List<String> unknown = PrivacyAuthorizationCatalogPolicy.unknownItems(privacyAuthorizationCatalog, codes);
        if (!enforced) {
            // 兼容模式（COMPAT_MODE）：不校验目录版本、不校验目录外项，只告警（车端存量报文可继续上报）
            if (!versionReported) {
                log.warn("隐私授权上报缺少 catalogVersion，按 COMPAT_MODE 回退旧行为（不校验目录版本/目录外项）: "
                        + "reportId={}, current={}", request.getReportId(), privacyAuthorizationCatalog.catalogVersion());
            } else if (!privacyAuthorizationCatalog.versionMatches(reportedCatalogVersion)) {
                log.warn("隐私授权目录版本不一致（COMPAT_MODE 仅告警）: reportId={}, reported={}, current={}",
                        request.getReportId(), reportedCatalogVersion, privacyAuthorizationCatalog.catalogVersion());
            }
            if (!unknown.isEmpty()) {
                log.warn("隐私授权上报包含目录外授权项（COMPAT_MODE 仅告警）: reportId={}, items={}",
                        request.getReportId(), unknown);
            }
            return;
        }
        if (!privacyAuthorizationCatalog.versionMatches(reportedCatalogVersion)) {
            // G-1 规则 1：目录版本不一致 → 401119，整批拒绝、不写任何行（车端拉取目录后重报）
            throw business(UserVehicleBindingErrorCode.CATALOG_VERSION_MISMATCH);
        }
        if (!unknown.isEmpty()) {
            // G-1 规则 2：目录外授权项 → 401118，整批拒绝、不写任何行
            throw business(UserVehicleBindingErrorCode.PRIVACY_ITEM_NOT_APPROVED);
        }
    }

    /**
     * 目录强校验是否生效（终审修复 B-1：{@code enabled} 与 {@code enforce-catalog} 同时为 true 才生效）。
     * <p>
     * 任一开关为 false 即进入 {@code COMPAT_MODE}：{@code catalogVersion} 允许缺失，
     * 目录版本与目录外授权项均只告警不拒绕，回退 CR-017 旧行为。
     *
     * @return true=目录强校验生效
     */
    private boolean isCatalogEnforced() {
        return privacyAuthorizationCatalog != null
                && properties.getPrivacyAuthorization().isEnabled()
                && properties.getPrivacyAuthorization().isEnforceCatalog();
    }

    /**
     * 登录回执审计 reason：FAILED 落 {@code FAILED:<reason>}（缺失记 {@code FAILED:-}），
     * 绑定操作号与会话不一致时标注 {@code FAILED_BINDING_OP_MISMATCH}（E2 定稿不返回 4xx）
     *
     * @param request                   回执
     * @param bindingOperationMismatch  绑定操作号是否与会话不一致
     * @return 审计 reason
     */
    private String loginAuditReason(ReportIviLoginResultRequest request, boolean bindingOperationMismatch) {
        if (request.getLoginStatus() != IviLoginResultStatus.FAILED) {
            return request.getLoginStatus().name();
        }
        String base = "FAILED:".concat(request.getFailureReason() == null
                ? "-" : request.getFailureReason().name());
        return bindingOperationMismatch ? "FAILED_BINDING_OP_MISMATCH/".concat(base) : base;
    }

    private void saveAccountConsents(ActivationSession session, String accountId,
                                     ReportIviLoginResultRequest request, Instant now) {
        if (request.getAgreements() == null || request.getAgreements().isEmpty()) {
            throw business(UserVehicleBindingErrorCode.AGREEMENT_NOT_ACCEPTED);
        }
        validateAcceptances(request.getAgreements());
        Instant reportedAgreedAt = parseOccurredAt(request.getOccurredAt());
        for (AgreementAcceptanceRequest acceptance : request.getAgreements()) {
            String type = acceptance.getAgreementType().name();
            if (session.getAgreementRevisions() == null
                    || !session.getAgreementRevisions().contains(type + ":" + acceptance.getRevisionId())) {
                // 登录回执的协议修订必须与会话展示时一致，不得重新取最新版
                throw business(UserVehicleBindingErrorCode.AGREEMENT_NOT_ACCEPTED);
            }
            var existing = consentRepository.findAccountConsent(accountId, session.getVin(), type,
                    acceptance.getRevisionId(), request.getReportId());
            String payloadDigest = DigestUtils.sha256Hex(String.join("|", accountId, session.getVin(), type,
                    acceptance.getRevisionId(), request.getReportId()));
            if (existing.isPresent()) {
                if (!payloadDigest.equals(existing.get().getPayloadDigest())) {
                    throw business(UserVehicleBindingErrorCode.REPORT_CONFLICT);
                }
                continue;
            }
            consentRepository.insertAccountConsent(AccountAgreementConsent.builder()
                    .consentId(newId("CAS"))
                    .accountId(accountId)
                    .vin(session.getVin())
                    .agreementType(type)
                    .version(acceptance.getVersion())
                    .revisionId(acceptance.getRevisionId())
                    .language(acceptance.getLanguage())
                    .contentHash(acceptance.getContentHash())
                    .source(SOURCE_VEHICLE)
                    .reportId(request.getReportId())
                    .payloadDigest(payloadDigest)
                    .sourceSessionId(session.getSessionId())
                    .bindingOperationId(session.getBindingOperationId())
                    .reportedAgreedAt(reportedAgreedAt)
                    .receivedAt(now)
                    .build());
        }
    }

    private void markSessionSkipped(String vin, String sourceSessionId, Instant now) {
        if (isBlank(sourceSessionId)) {
            return;
        }
        var sessionOpt = activationSessionRepository.findBySessionId(sourceSessionId);
        if (sessionOpt.isEmpty()) {
            return;
        }
        ActivationSession session = sessionOpt.get();
        if (!vin.equals(session.getVin())
                || (session.getState() != ActivationSessionState.ISSUED
                && session.getState() != ActivationSessionState.SCANNED)) {
            return;
        }
        activationSessionRepository.markSkipped(session.getSessionId(), now,
                session.getRowVersion() == null ? 1 : session.getRowVersion());
    }

    private void validateAcceptances(List<AgreementAcceptanceRequest> agreements) {
        if (agreements == null || agreements.isEmpty()) {
            throw business(UserVehicleBindingErrorCode.AGREEMENT_NOT_ACCEPTED);
        }
        Set<String> types = new LinkedHashSet<>();
        for (AgreementAcceptanceRequest acceptance : agreements) {
            if (acceptance == null || acceptance.getAgreementType() == null
                    || !Boolean.TRUE.equals(acceptance.getAgreed())
                    || isBlank(acceptance.getRevisionId())) {
                throw business(UserVehicleBindingErrorCode.AGREEMENT_NOT_ACCEPTED);
            }
            types.add(acceptance.getAgreementType().name());
        }
        if (types.size() < 2) {
            throw business(UserVehicleBindingErrorCode.AGREEMENT_NOT_ACCEPTED);
        }
    }

    private String revisionsOf(List<AgreementAcceptanceRequest> agreements) {
        List<String> revisions = new ArrayList<>();
        for (AgreementAcceptanceRequest acceptance : agreements) {
            revisions.add(acceptance.getAgreementType().name() + ":" + acceptance.getRevisionId());
        }
        revisions.sort(String::compareTo);
        return String.join(",", revisions);
    }

    private Instant parseOccurredAt(String occurredAt) {
        if (isBlank(occurredAt)) {
            return null;
        }
        try {
            return Instant.parse(occurredAt.trim());
        } catch (DateTimeParseException e) {
            // 时间无效不冒充用户动作时间，标记为未提供
            log.warn("回执时间格式非法，按未提供处理: {}", occurredAt);
            return null;
        }
    }

    private void audit(String bizType, String bizId, String vin, String accountId, String fromState,
                       String toState, String operatorSource, String reason, Instant now) {
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
                .occurredAt(now)
                .build());
    }

    private BusinessException business(UserVehicleBindingErrorCode errorCode) {
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
