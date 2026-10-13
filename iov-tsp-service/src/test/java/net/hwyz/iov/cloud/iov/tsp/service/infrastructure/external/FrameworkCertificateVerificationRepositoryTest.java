package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.external;

import net.hwyz.iov.cloud.framework.security.crypto.client.PkiClient;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusDependencyUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusProtocolException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusTrustValidationException;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.CertificateDependencyUnavailableException;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.CertificateIdentity;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.CertificateStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.CertificateVerification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * framework 证书吊销适配单元测试（TSP-DSN-CR-011 §3/§6）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class FrameworkCertificateVerificationRepositoryTest {

    private static final Instant DECISION_AT = Instant.parse("2026-10-12T13:00:00Z");

    @Mock
    private ObjectProvider<PkiClient> pkiClientProvider;

    @Mock
    private PkiClient pkiClient;

    @InjectMocks
    private FrameworkCertificateVerificationRepository repository;

    private static CertificateIdentity identity() {
        return new CertificateIdentity("0a1b2c", "fingerprint", new byte[]{1, 2, 3});
    }

    private static PkiClient.CertificateStatusResult result(PkiClient.RevocationState state) {
        return new PkiClient.CertificateStatusResult(
                state, "HSM-0001", DECISION_AT.minusSeconds(60), DECISION_AT.plusSeconds(3600),
                DECISION_AT, DECISION_AT.plusSeconds(60), PkiClient.RevocationSource.OCSP, DECISION_AT);
    }

    @Test
    void goodState_shouldMapToValidAndPassThroughSubjectCommonName() {
        when(pkiClientProvider.getIfAvailable()).thenReturn(pkiClient);
        when(pkiClient.checkCertificateStatus(any())).thenReturn(result(PkiClient.RevocationState.GOOD));

        CertificateVerification verification = repository.verify(identity(), DECISION_AT);

        assertEquals(CertificateStatus.VALID, verification.status());
        assertEquals("OCSP", verification.source());
        assertEquals("HSM-0001", verification.subjectCommonName());
    }

    @Test
    void verify_shouldMapIdentityToFrameworkQueryContract() {
        when(pkiClientProvider.getIfAvailable()).thenReturn(pkiClient);
        when(pkiClient.checkCertificateStatus(any())).thenReturn(result(PkiClient.RevocationState.GOOD));
        CertificateIdentity identity = new CertificateIdentity("0a1b2c", "fingerprint", new byte[]{1, 2, 3});

        repository.verify(identity, DECISION_AT);

        ArgumentCaptor<PkiClient.CertificateStatusQuery> captor =
                ArgumentCaptor.forClass(PkiClient.CertificateStatusQuery.class);
        verify(pkiClient).checkCertificateStatus(captor.capture());
        PkiClient.CertificateStatusQuery query = captor.getValue();
        assertArrayEquals(new byte[]{1, 2, 3}, query.leafCertificateDer());
        assertEquals("a1b2c", query.expectedSerial());
        assertEquals("fingerprint", query.expectedSha256Fingerprint());
        assertEquals(DECISION_AT, query.decisionAt());
    }

    @Test
    void revokedState_shouldMapToRevoked() {
        when(pkiClientProvider.getIfAvailable()).thenReturn(pkiClient);
        when(pkiClient.checkCertificateStatus(any())).thenReturn(result(PkiClient.RevocationState.REVOKED));

        assertEquals(CertificateStatus.REVOKED, repository.verify(identity(), DECISION_AT).status());
    }

    @Test
    void unknownState_shouldMapToUnknown() {
        when(pkiClientProvider.getIfAvailable()).thenReturn(pkiClient);
        when(pkiClient.checkCertificateStatus(any())).thenReturn(result(PkiClient.RevocationState.UNKNOWN));

        assertEquals(CertificateStatus.UNKNOWN, repository.verify(identity(), DECISION_AT).status());
    }

    @Test
    void dependencyException_shouldMapToConnectWithCause() {
        when(pkiClientProvider.getIfAvailable()).thenReturn(pkiClient);
        CertificateStatusDependencyUnavailableException cause =
                new CertificateStatusDependencyUnavailableException("unreachable");
        when(pkiClient.checkCertificateStatus(any())).thenThrow(cause);

        CertificateDependencyUnavailableException thrown = assertThrows(CertificateDependencyUnavailableException.class,
                () -> repository.verify(identity(), DECISION_AT));

        assertEquals(CertificateDependencyUnavailableException.CATEGORY_CONNECT, thrown.getCategory());
        assertEquals(cause, thrown.getCause());
    }

    @Test
    void trustException_shouldMapToTls() {
        when(pkiClientProvider.getIfAvailable()).thenReturn(pkiClient);
        when(pkiClient.checkCertificateStatus(any()))
                .thenThrow(new CertificateStatusTrustValidationException("bad chain"));

        CertificateDependencyUnavailableException thrown = assertThrows(CertificateDependencyUnavailableException.class,
                () -> repository.verify(identity(), DECISION_AT));

        assertEquals(CertificateDependencyUnavailableException.CATEGORY_TLS, thrown.getCategory());
    }

    @Test
    void protocolException_shouldMapToProtocol() {
        when(pkiClientProvider.getIfAvailable()).thenReturn(pkiClient);
        when(pkiClient.checkCertificateStatus(any()))
                .thenThrow(new CertificateStatusProtocolException("bad response"));

        CertificateDependencyUnavailableException thrown = assertThrows(CertificateDependencyUnavailableException.class,
                () -> repository.verify(identity(), DECISION_AT));

        assertEquals(CertificateDependencyUnavailableException.CATEGORY_PROTOCOL, thrown.getCategory());
    }

    @Test
    void missingLeafCertificate_shouldFailClosedWithoutCallingPki() {
        CertificateIdentity noLeaf = new CertificateIdentity("0a1b2c", "fingerprint", null);

        CertificateDependencyUnavailableException thrown = assertThrows(CertificateDependencyUnavailableException.class,
                () -> repository.verify(noLeaf, DECISION_AT));

        assertEquals(CertificateDependencyUnavailableException.CATEGORY_CONFIGURATION, thrown.getCategory());
        verify(pkiClient, never()).checkCertificateStatus(any());
    }
}
