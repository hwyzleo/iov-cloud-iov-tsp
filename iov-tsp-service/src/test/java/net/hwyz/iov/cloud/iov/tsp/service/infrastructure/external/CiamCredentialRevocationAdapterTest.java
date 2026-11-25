package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.external;

import net.hwyz.iov.cloud.iov.tsp.api.external.CiamBindingRevocationClient;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.CredentialRevocationResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.DependencyUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CIAM 绑定撤销适配测试（IOV-TSP-DSN-CR-021 TC021-10～TC021-12）。
 */
@ExtendWith(MockitoExtension.class)
class CiamCredentialRevocationAdapterTest {

    private static final Instant NOW = Instant.parse("2026-11-25T02:00:00Z");

    @Mock
    private CiamBindingRevocationClient client;

    @Test
    @DisplayName("成功信封：status=SUCCEEDED 才视为收敛，occurredAt 带 Z")
    void successEnvelope() {
        when(client.revokeBinding(any())).thenReturn(new CiamBindingRevocationClient.RevokeBindingEnvelope(
                "000000", "success", new CiamBindingRevocationClient.RevokeBindingData(
                "UBO-1", "REL-1", "SUCCEEDED", 2)));

        CredentialRevocationResult result = new CiamCredentialRevocationAdapter(client)
                .revoke("UBO-1", "REL-1", "acc-1", "LSVAA1234567890AB", NOW, "CUSTOMER_VERIFIED_REQUEST");

        assertThat(result.revokedSessionCount()).isEqualTo(2);
        ArgumentCaptor<CiamBindingRevocationClient.RevokeBindingRequest> captor =
                ArgumentCaptor.forClass(CiamBindingRevocationClient.RevokeBindingRequest.class);
        verify(client).revokeBinding(captor.capture());
        assertThat(captor.getValue().occurredAt()).isEqualTo("2026-11-25T02:00:00Z");
        assertThat(captor.getValue().operationId()).isEqualTo("UBO-1");
    }

    @Test
    @DisplayName("非成功码：不伪造成功")
    void failureEnvelopeRejected() {
        when(client.revokeBinding(any())).thenReturn(new CiamBindingRevocationClient.RevokeBindingEnvelope(
                "701002", "系统内部错误", null));

        assertThatThrownBy(() -> new CiamCredentialRevocationAdapter(client)
                .revoke("UBO-1", "REL-1", "acc-1", "LSVAA1234567890AB", NOW, null))
                .isInstanceOf(DependencyUnavailableException.class);
    }

    @Test
    @DisplayName("客户调用异常/超时：包装为依赖不可用，不返回伪成功")
    void clientExceptionRejected() {
        when(client.revokeBinding(any())).thenThrow(new IllegalStateException("timeout"));

        assertThatThrownBy(() -> new CiamCredentialRevocationAdapter(client)
                .revoke("UBO-1", "REL-1", "acc-1", "LSVAA1234567890AB", NOW, null))
                .isInstanceOf(DependencyUnavailableException.class);
    }
}
