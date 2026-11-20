package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ActivationSessionState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 一次性会话状态语义测试（IOV-TSP-DSN-CR-017 TC017-02）。
 */
class ActivationSessionTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    private ActivationSession session(ActivationSessionState state, Instant expiresAt, String claimedAccount) {
        return ActivationSession.builder()
                .sessionId("SES-1")
                .vin("LSVAA1234567890AB")
                .state(state)
                .expiresAt(expiresAt)
                .claimedAccountId(claimedAccount)
                .build();
    }

    @Test
    @DisplayName("到期判定使用服务端时刻，到期边界外拒绝")
    void expiredAfterDeadline() {
        assertThat(session(ActivationSessionState.ISSUED, NOW.minusSeconds(1), null).isExpired(NOW)).isTrue();
        assertThat(session(ActivationSessionState.ISSUED, NOW.plusSeconds(1), null).isExpired(NOW)).isFalse();
    }

    @Test
    @DisplayName("取消/跳过/已绑定会话不可认领")
    void notClaimableForTerminalStates() {
        Instant future = NOW.plusSeconds(60);
        assertThat(session(ActivationSessionState.CANCELLED, future, null).claimableBy("A1", NOW)).isFalse();
        assertThat(session(ActivationSessionState.SKIPPED, future, null).claimableBy("A1", NOW)).isFalse();
        assertThat(session(ActivationSessionState.BOUND, future, "A1").claimableBy("A1", NOW)).isFalse();
    }

    @Test
    @DisplayName("同一账号可重复领取，其他账号不可占用")
    void claimOnlyByFirstAccount() {
        Instant future = NOW.plusSeconds(60);
        ActivationSession scanned = session(ActivationSessionState.SCANNED, future, "A1");
        assertThat(scanned.claimableBy("A1", NOW)).isTrue();
        assertThat(scanned.claimableBy("A2", NOW)).isFalse();
    }

    @Test
    @DisplayName("确认必须由认领账号在有效期内且处于 SCANNED")
    void confirmableOnlyForClaimantInWindow() {
        Instant future = NOW.plusSeconds(60);
        ActivationSession scanned = session(ActivationSessionState.SCANNED, future, "A1");
        assertThat(scanned.confirmableBy("A1", NOW)).isTrue();
        assertThat(scanned.confirmableBy("A2", NOW)).isFalse();
        assertThat(session(ActivationSessionState.SCANNED, NOW.minusSeconds(1), "A1").confirmableBy("A1", NOW)).isFalse();
        assertThat(session(ActivationSessionState.ISSUED, future, "A1").confirmableBy("A1", NOW)).isFalse();
    }
}
