package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.hwyz.iov.cloud.framework.security.crypto.client.PkiClient;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusDependencyUnavailableException;
import net.hwyz.iov.cloud.iov.tsp.api.vo.DeviceAdmissionCheckVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.DeviceAdmissionResultVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.AdmissionDecision;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.AdmissionReason;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CertificateStatus;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.DeviceAdmissionAppService;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.DeviceAdmissionLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.DeviceAdmissionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.service.DeviceAdmissionDomainService;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.external.FrameworkCertificateVerificationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.metrics.DeviceAdmissionMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 设备接入准入集成测试（TSP-DSN-CR-011 §11）
 * <p>
 * 覆盖 controller → app → domain → framework 适配 的完整 TSP 链路（framework 网络出口 mock），
 * 锁定新版 framework 契约（leafCertificateDer/expectedSerial/expectedSha256Fingerprint/decisionAt
 * 与返回 subjectCommonName）以及身份不一致、吊销、依赖故障三类 reason 的独立归类。
 *
 * @author hwyz_leo
 */
class DeviceAdmissionAdmissionIntegrationTest {

    private static final String UID = "HSM-0001";
    private static final String SN = "SN-0001";
    private static final String VIN = "VIN0000000000001";
    private static final Instant DECISION_AT = Instant.parse("2026-10-12T13:00:00Z");

    private DeviceAdmissionRepository deviceAdmissionRepository;
    private DeviceAdmissionLogRepository deviceAdmissionLogRepository;
    private PkiClient pkiClient;
    private ServiceDeviceAdmissionController controller;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        deviceAdmissionRepository = mock(DeviceAdmissionRepository.class);
        deviceAdmissionLogRepository = mock(DeviceAdmissionLogRepository.class);
        pkiClient = mock(PkiClient.class);

        ObjectProvider<PkiClient> pkiClientProvider = mock(ObjectProvider.class);
        when(pkiClientProvider.getIfAvailable()).thenReturn(pkiClient);

        FrameworkCertificateVerificationRepository certificateVerificationRepository =
                new FrameworkCertificateVerificationRepository(pkiClientProvider);
        DeviceAdmissionDomainService domainService = new DeviceAdmissionDomainService(
                deviceAdmissionRepository, deviceAdmissionLogRepository, certificateVerificationRepository);

        ObjectProvider<io.micrometer.core.instrument.MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);
        when(meterRegistryProvider.getIfAvailable()).thenReturn(new SimpleMeterRegistry());
        DeviceAdmissionAppService appService =
                new DeviceAdmissionAppService(domainService, new DeviceAdmissionMetrics(meterRegistryProvider));

        controller = new ServiceDeviceAdmissionController(appService);
    }

    private void givenActiveDeviceAndBinding() {
        when(deviceAdmissionRepository.findByHsm(UID)).thenReturn(Optional.of(
                new DeviceAdmissionRepository.TboxInfo(1L, SN, UID, 1, DECISION_AT.minusSeconds(60))));
        when(deviceAdmissionRepository.findActiveBindingsBySn(SN)).thenReturn(List.of(
                new DeviceAdmissionRepository.BindingInfo(VIN, 7L, DECISION_AT.minusSeconds(120))));
    }

    private DeviceAdmissionCheckVo request() {
        return DeviceAdmissionCheckVo.builder()
                .uid(UID)
                .certSerial("0a1b2c")
                .certFingerprint("fingerprint")
                .leafCertificateDer(Base64.getEncoder()
                        .encodeToString("leaf-der".getBytes(StandardCharsets.UTF_8)))
                .clientId("client-1")
                .sourceIp("10.0.0.1")
                .ts(DECISION_AT)
                .build();
    }

    private PkiClient.CertificateStatusResult certificateResult(PkiClient.RevocationState state, String subjectCommonName) {
        return new PkiClient.CertificateStatusResult(
                state, subjectCommonName, DECISION_AT.minusSeconds(60), DECISION_AT.plusSeconds(3600),
                DECISION_AT, DECISION_AT.plusSeconds(60), PkiClient.RevocationSource.OCSP, DECISION_AT);
    }

    @Test
    void goodCertificateWithMatchingCn_shouldAllow() {
        givenActiveDeviceAndBinding();
        when(pkiClient.checkCertificateStatus(any()))
                .thenReturn(certificateResult(PkiClient.RevocationState.GOOD, " hsm-0001 "));

        DeviceAdmissionResultVo result = controller.decide(request());

        assertEquals(AdmissionDecision.ALLOW, result.getDecision());
        assertEquals(AdmissionReason.NONE, result.getReason());
        assertEquals(VIN, result.getVin());
        assertEquals(CertificateStatus.VALID, result.getCertStatus());
    }

    @Test
    void goodCertificateWithMismatchedCn_shouldDenyIdentityMismatch() {
        givenActiveDeviceAndBinding();
        when(pkiClient.checkCertificateStatus(any()))
                .thenReturn(certificateResult(PkiClient.RevocationState.GOOD, "HSM-OTHER"));

        DeviceAdmissionResultVo result = controller.decide(request());

        assertEquals(AdmissionDecision.DENY, result.getDecision());
        assertEquals(AdmissionReason.IDENTITY_MISMATCH, result.getReason());
        assertEquals(CertificateStatus.VALID, result.getCertStatus());
    }

    @Test
    void goodCertificateWithMissingCn_shouldDenyIdentityMismatch() {
        givenActiveDeviceAndBinding();
        when(pkiClient.checkCertificateStatus(any()))
                .thenReturn(certificateResult(PkiClient.RevocationState.GOOD, null));

        DeviceAdmissionResultVo result = controller.decide(request());

        assertEquals(AdmissionDecision.DENY, result.getDecision());
        assertEquals(AdmissionReason.IDENTITY_MISMATCH, result.getReason());
    }

    @Test
    void revokedCertificate_shouldDenyCertRevoked() {
        givenActiveDeviceAndBinding();
        when(pkiClient.checkCertificateStatus(any()))
                .thenReturn(certificateResult(PkiClient.RevocationState.REVOKED, UID));

        DeviceAdmissionResultVo result = controller.decide(request());

        assertEquals(AdmissionDecision.DENY, result.getDecision());
        assertEquals(AdmissionReason.CERT_REVOKED, result.getReason());
        assertEquals(CertificateStatus.REVOKED, result.getCertStatus());
    }

    @Test
    void pkiDependencyUnavailable_shouldDenyDependencyUnavailable() {
        givenActiveDeviceAndBinding();
        when(pkiClient.checkCertificateStatus(any()))
                .thenThrow(new CertificateStatusDependencyUnavailableException("unreachable"));

        DeviceAdmissionResultVo result = controller.decide(request());

        assertEquals(AdmissionDecision.DENY, result.getDecision());
        assertEquals(AdmissionReason.DEPENDENCY_UNAVAILABLE, result.getReason());
        assertEquals(CertificateStatus.UNKNOWN, result.getCertStatus());
        assertNull(result.getVin());
        org.mockito.Mockito.verify(deviceAdmissionLogRepository).save(any());
        org.mockito.Mockito.verify(pkiClient, org.mockito.Mockito.never()).queryCertificate(anyString());
    }
}
