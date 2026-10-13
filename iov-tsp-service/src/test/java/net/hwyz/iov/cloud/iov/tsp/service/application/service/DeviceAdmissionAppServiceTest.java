package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.AdmissionDecision;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.AdmissionReason;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CertificateStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.DeviceStatus;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.DeviceAdmissionCheckCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.DeviceAdmissionCheckResult;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.DeviceAdmissionParamInvalidException;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmission;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmissionResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.service.DeviceAdmissionDomainService;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.metrics.DeviceAdmissionMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 设备接入准入应用服务单元测试（TSP-DSN-CR-011）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class DeviceAdmissionAppServiceTest {

    @Mock
    private DeviceAdmissionDomainService deviceAdmissionDomainService;

    @Mock
    private DeviceAdmissionMetrics deviceAdmissionMetrics;

    @InjectMocks
    private DeviceAdmissionAppService deviceAdmissionAppService;

    private static String validLeafBase64() {
        return Base64.getEncoder().encodeToString("leaf-certificate-der".getBytes(StandardCharsets.UTF_8));
    }

    private static DeviceAdmissionCheckCmd cmd(String leafDer) {
        return DeviceAdmissionCheckCmd.builder()
                .uid("HSM-0001")
                .certSerial("0a1b2c")
                .certFingerprint("fingerprint")
                .leafCertificateDer(leafDer)
                .clientId("client-1")
                .sourceIp("10.0.0.1")
                .ts(Instant.parse("2026-10-12T13:00:00Z"))
                .build();
    }

    @Test
    void decide_shouldMapResultAndRecordMetrics() {
        DeviceAdmission admission = DeviceAdmission.builder()
                .vin("VIN0000000000001")
                .admissionResult(DeviceAdmissionResult.ALLOW)
                .reason(net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.AdmissionReason.NONE)
                .bindStatus(net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.BindStatus.BOUND)
                .certStatus(net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.CertificateStatus.VALID)
                .deviceStatus(net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceStatus.PRE_ACTIVE)
                .bindVersion(7L)
                .revocationSource("OCSP")
                .pkiLatencyMs(12L)
                .updatedAt(Instant.parse("2026-10-12T13:00:00Z"))
                .build();
        when(deviceAdmissionDomainService.decide(any())).thenReturn(admission);

        DeviceAdmissionCheckResult result = deviceAdmissionAppService.decide(cmd(validLeafBase64()), "VAGW");

        assertEquals(AdmissionDecision.ALLOW, result.getDecision());
        assertEquals(AdmissionReason.NONE, result.getReason());
        assertEquals(BindStatus.BOUND, result.getBindStatus());
        assertEquals(CertificateStatus.VALID, result.getCertStatus());
        assertEquals(DeviceStatus.PRE_ACTIVE, result.getDeviceStatus());
        assertEquals(7L, result.getBindVersion());
        assertNotNull(result.getUpdatedAt());

        verify(deviceAdmissionMetrics).decision(eq("ALLOW"), eq("NONE"));
        verify(deviceAdmissionMetrics).latency(anyLong());
        verify(deviceAdmissionMetrics).pkiQuery(eq("OCSP"), eq("VALID"));
        verify(deviceAdmissionMetrics).pkiLatency(eq("OCSP"), anyLong());
    }

    @Test
    void decide_invalidBase64_shouldRejectAsParamInvalid() {
        assertThrows(DeviceAdmissionParamInvalidException.class,
                () -> deviceAdmissionAppService.decide(cmd("not-a-base64-!!!"), "VAGW"));
    }

    @Test
    void decide_oversizedLeafCertificate_shouldRejectAsParamInvalid() {
        byte[] oversized = new byte[9 * 1024];
        String encoded = Base64.getEncoder().encodeToString(oversized);

        assertThrows(DeviceAdmissionParamInvalidException.class,
                () -> deviceAdmissionAppService.decide(cmd(encoded), "VAGW"));
    }

    @Test
    void decide_identityMismatch_shouldRecordIdentityMetricOnly() {
        DeviceAdmission admission = DeviceAdmission.builder()
                .admissionResult(DeviceAdmissionResult.DENY)
                .reason(net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.AdmissionReason.IDENTITY_MISMATCH)
                .certStatus(net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.CertificateStatus.VALID)
                .dependencyOutcome("IDENTITY_MISMATCH")
                .pkiLatencyMs(3L)
                .build();
        when(deviceAdmissionDomainService.decide(any())).thenReturn(admission);

        deviceAdmissionAppService.decide(cmd(validLeafBase64()), "VAGW");

        verify(deviceAdmissionMetrics).identityMismatch();
        verify(deviceAdmissionMetrics, org.mockito.Mockito.never()).revoked();
        verify(deviceAdmissionMetrics, org.mockito.Mockito.never()).dependencyUnavailable(anyString());
    }

    @Test
    void decide_revokedAndDependencyUnavailable_shouldRecordSeparateMetrics() {
        DeviceAdmission admission = DeviceAdmission.builder()
                .admissionResult(DeviceAdmissionResult.DENY)
                .reason(net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.AdmissionReason.CERT_REVOKED)
                .certStatus(net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.CertificateStatus.REVOKED)
                .revocationSource("CRL")
                .pkiLatencyMs(5L)
                .build();
        when(deviceAdmissionDomainService.decide(any())).thenReturn(admission);

        deviceAdmissionAppService.decide(cmd(validLeafBase64()), "VAGW");

        verify(deviceAdmissionMetrics).revoked();
        verify(deviceAdmissionMetrics).pkiQuery(eq("CRL"), eq("REVOKED"));
        verify(deviceAdmissionMetrics, org.mockito.Mockito.never()).dependencyUnavailable(anyString());
    }
}
