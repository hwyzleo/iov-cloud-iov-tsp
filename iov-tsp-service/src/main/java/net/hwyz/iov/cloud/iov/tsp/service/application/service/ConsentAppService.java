package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ActivationSessionState;
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
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleAuditRecord;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationSessionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ConsentRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.PrivacyAuthorizationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleAuditRepository;
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
    private final ActivationLoginTicketAppService activationLoginTicketAppService;
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
     * 可信车机登录结果回报：登录成功时按 Account+VIN 记录两项协议同意
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
                || session.getBindingOperationId() == null
                || !session.getBindingOperationId().equals(request.getBindingOperationId())) {
            throw business(UserVehicleBindingErrorCode.SESSION_INVALID);
        }
        String accountId = session.getClaimedAccountId();
        if (accountId == null) {
            throw business(UserVehicleBindingErrorCode.INVALID_REQUEST);
        }
        Instant now = clock.instant();
        // D019-05：登录成功写入与未消费票据作废必须在同一事务，避免出现“已登录仍可换取票据”的窗口
        ReportAckVo ack = transactionTemplate.execute(status -> {
            boolean duplicateReport = request.getReportId().equals(session.getIviLoginReportId());
            if (!duplicateReport) {
                if (!activationSessionRepository.reportIviLoginResult(session.getSessionId(), request.getLoginStatus(),
                        request.getReportId(), now)) {
                    throw business(UserVehicleBindingErrorCode.REPORT_CONFLICT);
                }
            }
            if (request.getLoginStatus() == net.hwyz.iov.cloud.iov.tsp.api.vo.enums.IviLoginResultStatus.SUCCEEDED) {
                saveAccountConsents(session, accountId, request, now);
                activationLoginTicketAppService.revokeIssuedOnLoginSuccess(session.getSessionId(), vin, now);
            }
            audit("LOGIN_RECEIPT", request.getReportId(), vin, accountId, null, request.getLoginStatus().name(),
                    SOURCE_VEHICLE, null, now);
            return ReportAckVo.builder().reportId(request.getReportId()).accepted(true).build();
        });
        if (ack == null) {
            throw business(UserVehicleBindingErrorCode.INVALID_REQUEST);
        }
        return ack;
    }

    /**
     * 补报隐私授权（Account + VIN + 授权项，按来源版本收敛）
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
        for (ReportPrivacyAuthorizationRequest.AuthorizationItem item : request.getItems()) {
            if (item == null || isBlank(item.getAuthorizationItem()) || item.getState() == null
                    || item.getVersion() == null) {
                throw business(UserVehicleBindingErrorCode.INVALID_REQUEST);
            }
            String authorizationItem = item.getAuthorizationItem().trim();
            String payloadDigest = DigestUtils.sha256Hex(String.join("|", accountId, vin, authorizationItem,
                    item.getState().name(), String.valueOf(item.getVersion())));
            var existing = privacyAuthorizationRepository.find(accountId, vin, authorizationItem);
            if (existing.isPresent()) {
                PrivacyAuthorization current = existing.get();
                if (request.getReportId().equals(current.getReportId())
                        && !payloadDigest.equals(current.getPayloadDigest())) {
                    throw business(UserVehicleBindingErrorCode.REPORT_CONFLICT);
                }
                long currentVersion = current.getVersion() == null ? 0L : current.getVersion();
                if (item.getVersion() <= currentVersion) {
                    // 重复或旧版本不得覆盖新状态
                    continue;
                }
                PrivacyAuthorization update = PrivacyAuthorization.builder()
                        .authorizationId(current.getAuthorizationId())
                        .accountId(accountId)
                        .vin(vin)
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
        audit("PRIVACY", request.getReportId(), vin, accountId, null, "PRIVACY_REPORT", SOURCE_VEHICLE, null, now);
        return ReportAckVo.builder().reportId(request.getReportId()).accepted(true).build();
    }

    // ================= 内部实现 =================

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
