package net.hwyz.iov.cloud.iov.tsp.service.application.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 一次性登录票据应用服务测试（IOV-TSP-DSN-CR-019 TC019-01～TC019-10）。
 * <p>
 * 覆盖签发前置矩阵、跨车拒绝、重签作废、TTL/Clock 判定、登录成功禁签、
 * 会话级限流、原子消费单次成功、已消费/过期/不存在/作废拒绝，以及审计脱敏。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActivationLoginTicketAppServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");
    private static final String VIN = "LSVAA1234567890AB";
    private static final String OTHER_VIN = "LSVAA9876543210ZZ";
    private static final String ACCOUNT = "account-1";
    private static final String SESSION_ID = "SES-1";
    private static final String OPERATION_ID = "BOP-1";
    private static final String RELATION_ID = "REL-1";

    @Mock
    private ActivationSessionRepository activationSessionRepository;
    @Mock
    private BindingOperationRepository bindingOperationRepository;
    @Mock
    private ActivationLoginTicketRepository activationLoginTicketRepository;
    @Mock
    private UserVehicleAuditRepository userVehicleAuditRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private TspUserVehicleProperties properties;
    private ActivationLoginTicketAppService appService;

    @BeforeEach
    void setUp() {
        properties = new TspUserVehicleProperties();
        properties.getLoginTicket().setTtl(Duration.ofSeconds(180));
        properties.getLoginTicket().setPerSessionPerMinute(3);
        properties.getLoginTicket().setPerSessionMaxIssue(10);
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        appService = newService();
        lenient().when(activationSessionRepository.findBySessionId(SESSION_ID))
                .thenReturn(Optional.of(boundSession()));
        lenient().when(bindingOperationRepository.findByOperationId(OPERATION_ID))
                .thenReturn(Optional.of(BindingOperation.builder().operationId(OPERATION_ID)
                        .resultRelationId(RELATION_ID).build()));
    }

    private ActivationLoginTicketAppService newService() {
        return new ActivationLoginTicketAppService(activationSessionRepository, bindingOperationRepository,
                activationLoginTicketRepository, userVehicleAuditRepository, properties,
                new TransactionTemplate(transactionManager), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private ActivationSession boundSession() {
        return ActivationSession.builder()
                .sessionId(SESSION_ID)
                .vin(VIN)
                .claimedAccountId(ACCOUNT)
                .state(ActivationSessionState.BOUND)
                .bindingOperationId(OPERATION_ID)
                .resultExpiresAt(NOW.plus(Duration.ofMinutes(30)))
                .expiresAt(NOW.plus(Duration.ofMinutes(10)))
                .build();
    }

    // ================= 签发 =================

    @Test
    @DisplayName("TC019-01：满足条件可签发，票据原值只出现一次且仅落摘要")
    void issueSucceeds() {
        when(activationLoginTicketRepository.maxIssueSeqBySession(SESSION_ID)).thenReturn(0);
        ArgumentCaptor<ActivationLoginTicket> captor = ArgumentCaptor.forClass(ActivationLoginTicket.class);

        LoginTicketVo vo = appService.issue(VIN, SESSION_ID);

        assertThat(vo.getTicket()).isNotBlank().hasSize(43);
        assertThat(vo.getExpiresAt()).isEqualTo(NOW.plusSeconds(180));
        assertThat(vo.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(vo.getRelationId()).isEqualTo(RELATION_ID);
        verify(activationLoginTicketRepository).revokeIssuedBySession(SESSION_ID, NOW);
        verify(activationLoginTicketRepository).insert(captor.capture());
        ActivationLoginTicket saved = captor.getValue();
        assertThat(saved.getTicketHash()).isEqualTo(DigestUtils.sha256Hex(vo.getTicket())).hasSize(64);
        assertThat(saved.getTicketHash()).isNotEqualTo(vo.getTicket());
        assertThat(saved.getAccountId()).isEqualTo(ACCOUNT);
        assertThat(saved.getVin()).isEqualTo(VIN);
        assertThat(saved.getBindingOperationId()).isEqualTo(OPERATION_ID);
        assertThat(saved.getIssueSeq()).isEqualTo(1);
        assertThat(saved.getState()).isEqualTo(LoginTicketState.ISSUED);
    }

    @Test
    @DisplayName("TC019-01：未绑定会话拒绝签发（401115），不降级签发")
    void issueRejectsNotBoundSession() {
        ActivationSession session = boundSession();
        session.setState(ActivationSessionState.SCANNED);
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));

        assertTicketError(() -> appService.issue(VIN, SESSION_ID),
                ActivationLoginTicketErrorCode.LOGIN_TICKET_UNAVAILABLE);
        verify(activationLoginTicketRepository, never()).insert(any());
        verify(activationLoginTicketRepository, never()).revokeIssuedBySession(anyString(), any());
    }

    @Test
    @DisplayName("TC019-01：绑定操作缺失拒绝签发")
    void issueRejectsMissingBindingOperation() {
        ActivationSession session = boundSession();
        session.setBindingOperationId(null);
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));

        assertTicketError(() -> appService.issue(VIN, SESSION_ID),
                ActivationLoginTicketErrorCode.LOGIN_TICKET_UNAVAILABLE);
        verify(activationLoginTicketRepository, never()).insert(any());
    }

    @Test
    @DisplayName("TC019-01：登录已完成拒绝签发")
    void issueRejectsLoginSucceeded() {
        ActivationSession session = boundSession();
        session.setIviLoginState(IviLoginResultStatus.SUCCEEDED);
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));

        assertTicketError(() -> appService.issue(VIN, SESSION_ID),
                ActivationLoginTicketErrorCode.LOGIN_TICKET_UNAVAILABLE);
        verify(activationLoginTicketRepository, never()).insert(any());
    }

    @Test
    @DisplayName("TC019-01：结果恢复窗口已过期拒绝签发")
    void issueRejectsExpiredRecoveryWindow() {
        ActivationSession session = boundSession();
        session.setResultExpiresAt(NOW.minusSeconds(1));
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));

        assertTicketError(() -> appService.issue(VIN, SESSION_ID),
                ActivationLoginTicketErrorCode.LOGIN_TICKET_UNAVAILABLE);
        verify(activationLoginTicketRepository, never()).insert(any());
    }

    @Test
    @DisplayName("TC019-01：会话本身过期拒绝签发")
    void issueRejectsExpiredSession() {
        ActivationSession session = boundSession();
        session.setExpiresAt(NOW.minusSeconds(1));
        when(activationSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));

        assertTicketError(() -> appService.issue(VIN, SESSION_ID),
                ActivationLoginTicketErrorCode.LOGIN_TICKET_UNAVAILABLE);
    }

    @Test
    @DisplayName("TC019-05：跨车申请拒绝（401101），不泄露会话事实")
    void issueRejectsCrossVehicle() {
        assertTicketError(() -> appService.issue(OTHER_VIN, SESSION_ID),
                UserVehicleBindingErrorCode.SESSION_INVALID);
        verify(activationLoginTicketRepository, never()).insert(any());
    }

    @Test
    @DisplayName("TC019-01：会话不存在按无效处理（401101）")
    void issueRejectsUnknownSession() {
        when(activationSessionRepository.findBySessionId("SES-X")).thenReturn(Optional.empty());

        assertTicketError(() -> appService.issue(VIN, "SES-X"),
                UserVehicleBindingErrorCode.SESSION_INVALID);
    }

    @Test
    @DisplayName("TC019-09：签发开关关闭即拒绝（回滚开关），不写票据")
    void issueRejectedWhenDisabled() {
        properties.getLoginTicket().setEnabled(false);

        assertTicketError(() -> appService.issue(VIN, SESSION_ID),
                ActivationLoginTicketErrorCode.LOGIN_TICKET_UNAVAILABLE);
        verify(activationLoginTicketRepository, never()).insert(any());
    }

    @Test
    @DisplayName("TC019-02：重复申请作废旧票并递增签发序号")
    void reissueRevokesPreviousAndIncrementsSeq() {
        when(activationLoginTicketRepository.maxIssueSeqBySession(SESSION_ID)).thenReturn(1);
        ArgumentCaptor<ActivationLoginTicket> captor = ArgumentCaptor.forClass(ActivationLoginTicket.class);

        appService.issue(VIN, SESSION_ID);

        verify(activationLoginTicketRepository).revokeIssuedBySession(SESSION_ID, NOW);
        verify(activationLoginTicketRepository).insert(captor.capture());
        assertThat(captor.getValue().getIssueSeq()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC019-08：会话累计签发次数超限返回可重试错误（701003）")
    void issueRejectsWhenSessionMaxIssueReached() {
        when(activationLoginTicketRepository.maxIssueSeqBySession(SESSION_ID)).thenReturn(10);

        assertTicketError(() -> appService.issue(VIN, SESSION_ID), ActivationLoginTicketErrorCode.RATE_LIMITED);
        verify(activationLoginTicketRepository, never()).insert(any());
    }

    @Test
    @DisplayName("TC019-08：会话每分钟签发频率超限返回可重试错误（701003）")
    void issueRejectsWhenPerMinuteLimitReached() {
        when(activationLoginTicketRepository.maxIssueSeqBySession(SESSION_ID)).thenReturn(0);

        appService.issue(VIN, SESSION_ID);
        appService.issue(VIN, SESSION_ID);
        appService.issue(VIN, SESSION_ID);

        assertTicketError(() -> appService.issue(VIN, SESSION_ID), ActivationLoginTicketErrorCode.RATE_LIMITED);
    }

    @Test
    @DisplayName("TC019-07：审计不含票据原值与账号明文，只记录票据标识与会话摘要")
    void issueAuditIsDesensitized() {
        when(activationLoginTicketRepository.maxIssueSeqBySession(SESSION_ID)).thenReturn(0);
        ArgumentCaptor<UserVehicleAuditRecord> captor = ArgumentCaptor.forClass(UserVehicleAuditRecord.class);

        LoginTicketVo vo = appService.issue(VIN, SESSION_ID);

        verify(userVehicleAuditRepository).insert(captor.capture());
        UserVehicleAuditRecord record = captor.getValue();
        assertThat(record.getBizType()).isEqualTo("TICKET_ISSUED");
        assertThat(record.getBizId()).startsWith("TKT");
        assertThat(record.getAccountId()).isNull();
        assertThat(record.getVin()).isEqualTo("LSV***90AB");
        assertThat(record.toString()).doesNotContain(vo.getTicket());
    }

    // ================= 消费 =================

    private ActivationLoginTicket issuedTicket(Instant expiresAt) {
        return ActivationLoginTicket.builder()
                .ticketId("TKT-1")
                .ticketHash(DigestUtils.sha256Hex("TICKET-VALUE"))
                .sessionId(SESSION_ID)
                .bindingOperationId(OPERATION_ID)
                .vin(VIN)
                .accountId(ACCOUNT)
                .relationId(RELATION_ID)
                .state(LoginTicketState.ISSUED)
                .issuedAt(NOW.minusSeconds(10))
                .expiresAt(expiresAt)
                .issueSeq(1)
                .build();
    }

    @Test
    @DisplayName("TC019-03：有效票据消费成功，返回四要素且无业务副作用")
    void consumeSucceeds() {
        when(activationLoginTicketRepository.findByTicketHash(DigestUtils.sha256Hex("TICKET-VALUE")))
                .thenReturn(Optional.of(issuedTicket(NOW.plusSeconds(170))));
        when(activationLoginTicketRepository.consume(DigestUtils.sha256Hex("TICKET-VALUE"), "sec-ciam", NOW))
                .thenReturn(1);

        ConsumeLoginTicketResult result = appService.consume("TICKET-VALUE", "sec-ciam");

        assertThat(result.getAccountId()).isEqualTo(ACCOUNT);
        assertThat(result.getVin()).isEqualTo(VIN);
        assertThat(result.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(result.getBindingOperationId()).isEqualTo(OPERATION_ID);
        assertThat(result.getRelationId()).isEqualTo(RELATION_ID);
        assertThat(result.getExpiresAt()).isEqualTo(NOW.plusSeconds(170));
        // 消费不修改会话/关系/激活事实
        verify(activationSessionRepository, never()).lockBySessionId(anyString());
        verify(activationSessionRepository, never()).markBound(anyString(), anyString(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("TC019-03：调用方缺省时消费方记为配置值（sec-ciam）")
    void consumeUsesConfiguredConsumerWhenCallerAbsent() {
        when(activationLoginTicketRepository.findByTicketHash(DigestUtils.sha256Hex("TICKET-VALUE")))
                .thenReturn(Optional.of(issuedTicket(NOW.plusSeconds(170))));
        when(activationLoginTicketRepository.consume(DigestUtils.sha256Hex("TICKET-VALUE"), "sec-ciam", NOW))
                .thenReturn(1);

        appService.consume("TICKET-VALUE", null);

        verify(activationLoginTicketRepository).consume(DigestUtils.sha256Hex("TICKET-VALUE"), "sec-ciam", NOW);
    }

    @Test
    @DisplayName("TC019-03：并发消费仅一次成功，其余返回已消费（401116）")
    void consumeOnlyOnce() {
        ActivationLoginTicket consumed = issuedTicket(NOW.plusSeconds(170));
        consumed.setState(LoginTicketState.CONSUMED);
        when(activationLoginTicketRepository.findByTicketHash(DigestUtils.sha256Hex("TICKET-VALUE")))
                .thenReturn(Optional.of(issuedTicket(NOW.plusSeconds(170))));
        when(activationLoginTicketRepository.consume(anyString(), anyString(), any())).thenReturn(0);
        when(activationLoginTicketRepository.findByTicketHash(DigestUtils.sha256Hex("TICKET-VALUE")))
                .thenReturn(Optional.of(issuedTicket(NOW.plusSeconds(170))), Optional.of(consumed));

        assertTicketError(() -> appService.consume("TICKET-VALUE", "sec-ciam"),
                ActivationLoginTicketErrorCode.LOGIN_TICKET_CONSUMED);
    }

    @Test
    @DisplayName("TC019-05：过期票据拒绝（401117），不返回绑定事实")
    void consumeRejectsExpired() {
        when(activationLoginTicketRepository.findByTicketHash(anyString()))
                .thenReturn(Optional.of(issuedTicket(NOW)));

        assertTicketError(() -> appService.consume("TICKET-VALUE", "sec-ciam"),
                ActivationLoginTicketErrorCode.LOGIN_TICKET_EXPIRED);
        verify(activationLoginTicketRepository, never()).consume(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("TC019-05：不存在票据拒绝（401115）")
    void consumeRejectsUnknownTicket() {
        when(activationLoginTicketRepository.findByTicketHash(anyString())).thenReturn(Optional.empty());

        assertTicketError(() -> appService.consume("TICKET-VALUE", "sec-ciam"),
                ActivationLoginTicketErrorCode.LOGIN_TICKET_UNAVAILABLE);
    }

    @Test
    @DisplayName("TC019-09：已作废票据一律按不可用拒绝（401115）")
    void consumeRejectsRevoked() {
        ActivationLoginTicket revoked = issuedTicket(NOW.plusSeconds(170));
        revoked.setState(LoginTicketState.REVOKED);
        when(activationLoginTicketRepository.findByTicketHash(anyString())).thenReturn(Optional.of(revoked));

        assertTicketError(() -> appService.consume("TICKET-VALUE", "sec-ciam"),
                ActivationLoginTicketErrorCode.LOGIN_TICKET_UNAVAILABLE);
    }

    @Test
    @DisplayName("TC019-03：CAS 未命中且已跨过到期点时按过期拒绝")
    void consumeCasMissThenExpired() {
        when(activationLoginTicketRepository.findByTicketHash(anyString()))
                .thenReturn(Optional.of(issuedTicket(NOW.plusSeconds(170))), Optional.of(issuedTicket(NOW)));
        when(activationLoginTicketRepository.consume(anyString(), anyString(), any())).thenReturn(0);

        assertTicketError(() -> appService.consume("TICKET-VALUE", "sec-ciam"),
                ActivationLoginTicketErrorCode.LOGIN_TICKET_EXPIRED);
    }

    @Test
    @DisplayName("空票据按参数非法拒绝（401112）")
    void consumeRejectsBlankTicket() {
        assertTicketError(() -> appService.consume(" ", "sec-ciam"),
                UserVehicleBindingErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("TC019-10：登录成功回执联动作废会话未消费票据")
    void revokeOnLoginSuccess() {
        when(activationLoginTicketRepository.revokeIssuedBySession(SESSION_ID, NOW)).thenReturn(2);
        ArgumentCaptor<UserVehicleAuditRecord> captor = ArgumentCaptor.forClass(UserVehicleAuditRecord.class);

        int revoked = appService.revokeIssuedOnLoginSuccess(SESSION_ID, VIN, NOW);

        assertThat(revoked).isEqualTo(2);
        verify(userVehicleAuditRepository).insert(captor.capture());
        assertThat(captor.getValue().getBizType()).isEqualTo("TICKET_REJECTED");
        assertThat(captor.getValue().getReason()).isEqualTo("LOGIN_SUCCEEDED");
        assertThat(captor.getValue().getToState()).isEqualTo("REVOKED");
    }

    @Test
    @DisplayName("TC019-10：无可作废票据时静默返回 0，不写审计")
    void revokeOnLoginSuccessNoop() {
        when(activationLoginTicketRepository.revokeIssuedBySession(SESSION_ID, NOW)).thenReturn(0);

        assertThat(appService.revokeIssuedOnLoginSuccess(SESSION_ID, VIN, NOW)).isZero();
        verify(userVehicleAuditRepository, never()).insert(any());
    }

    private void assertTicketError(Runnable action,
                                   net.hwyz.iov.cloud.framework.common.exception.ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(expected.getCode()));
    }
}
