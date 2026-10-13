package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.service;

import net.hwyz.iov.cloud.iov.tsp.service.common.exception.CertificateDependencyUnavailableException;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.AdmissionReason;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.BindStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.CertificateStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.CertificateVerification;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmission;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmissionCommand;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmissionResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.CertificateVerificationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.DeviceAdmissionLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.DeviceAdmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 设备接入准入领域服务单元测试（TSP-DSN-CR-011 §11）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class DeviceAdmissionDomainServiceTest {

    private static final String UID = "HSM-0001";
    private static final String SN = "SN-0001";
    private static final String VIN = "VIN0000000000001";
    private static final Instant DECISION_AT = Instant.parse("2026-10-12T13:00:00Z");

    @Mock
    private DeviceAdmissionRepository deviceAdmissionRepository;

    @Mock
    private DeviceAdmissionLogRepository deviceAdmissionLogRepository;

    @Mock
    private CertificateVerificationRepository certificateVerificationRepository;

    @InjectMocks
    private DeviceAdmissionDomainService deviceAdmissionDomainService;

    private DeviceAdmissionCommand command() {
        return new DeviceAdmissionCommand(UID, "0a1b2c", "fingerprint",
                "leaf-der".getBytes(StandardCharsets.UTF_8), "client-1", "10.0.0.1", "VAGW", DECISION_AT);
    }

    private void givenTbox(Integer deviceStatus) {
        when(deviceAdmissionRepository.findByHsm(UID)).thenReturn(Optional.of(
                new DeviceAdmissionRepository.TboxInfo(1L, SN, UID, deviceStatus, DECISION_AT.minus(1, ChronoUnit.HOURS))));
    }

    private void givenBinding() {
        when(deviceAdmissionRepository.findActiveBindingsBySn(SN)).thenReturn(List.of(
                new DeviceAdmissionRepository.BindingInfo(VIN, 7L, DECISION_AT.minus(2, ChronoUnit.HOURS))));
    }

    private void givenCertificate(CertificateStatus status) {
        when(certificateVerificationRepository.verify(any(), any())).thenReturn(new CertificateVerification(
                status, DECISION_AT.minus(1, ChronoUnit.DAYS), DECISION_AT.plus(1, ChronoUnit.DAYS),
                "OCSP", DECISION_AT));
    }

    @Test
    void uidUnknown_shouldDenyWithoutCallingPki() {
        when(deviceAdmissionRepository.findByHsm(UID)).thenReturn(Optional.empty());

        DeviceAdmission result = deviceAdmissionDomainService.decide(command());

        assertEquals(DeviceAdmissionResult.DENY, result.getAdmissionResult());
        assertEquals(AdmissionReason.UID_UNKNOWN, result.getReason());
        assertEquals(BindStatus.UNKNOWN, result.getBindStatus());
        verifyNoInteractions(certificateVerificationRepository);
        verify(deviceAdmissionLogRepository).save(any(DeviceAdmission.class));
    }

    @Test
    void unbound_shouldDenyWithoutCallingPki() {
        givenTbox(1);
        when(deviceAdmissionRepository.findActiveBindingsBySn(SN)).thenReturn(List.of());

        DeviceAdmission result = deviceAdmissionDomainService.decide(command());

        assertEquals(DeviceAdmissionResult.DENY, result.getAdmissionResult());
        assertEquals(AdmissionReason.UNBOUND, result.getReason());
        assertEquals(BindStatus.UNBOUND, result.getBindStatus());
        verifyNoInteractions(certificateVerificationRepository);
    }

    @Test
    void bindingConflict_shouldDenyWithoutCallingPki() {
        givenTbox(1);
        when(deviceAdmissionRepository.findActiveBindingsBySn(SN)).thenReturn(List.of(
                new DeviceAdmissionRepository.BindingInfo(VIN, 7L, DECISION_AT),
                new DeviceAdmissionRepository.BindingInfo("VIN0000000000002", 8L, DECISION_AT)));

        DeviceAdmission result = deviceAdmissionDomainService.decide(command());

        assertEquals(DeviceAdmissionResult.DENY, result.getAdmissionResult());
        assertEquals(AdmissionReason.BINDING_CONFLICT, result.getReason());
        assertEquals(BindStatus.CONFLICT, result.getBindStatus());
        verifyNoInteractions(certificateVerificationRepository);
    }

    @Test
    void goodCertificateAndPreActive_shouldAllow() {
        givenTbox(1);
        givenBinding();
        givenCertificate(CertificateStatus.VALID);

        DeviceAdmission result = deviceAdmissionDomainService.decide(command());

        assertEquals(DeviceAdmissionResult.ALLOW, result.getAdmissionResult());
        assertEquals(AdmissionReason.NONE, result.getReason());
        assertEquals(BindStatus.BOUND, result.getBindStatus());
        assertEquals(CertificateStatus.VALID, result.getCertStatus());
        assertEquals(DeviceStatus.PRE_ACTIVE, result.getDeviceStatus());
        assertEquals(VIN, result.getVin());
        assertEquals(7L, result.getBindVersion());
        assertNotNull(result.getUpdatedAt());
        assertNotNull(result.getUidDigest());
        assertNotNull(result.getCertSerialDigest());
        assertNotNull(result.getSourceIpDigest());
        verify(deviceAdmissionLogRepository).save(any(DeviceAdmission.class));
    }

    @Test
    void goodCertificateAndActivating_shouldAllow() {
        givenTbox(5);
        givenBinding();
        givenCertificate(CertificateStatus.VALID);

        DeviceAdmission result = deviceAdmissionDomainService.decide(command());

        assertEquals(DeviceAdmissionResult.ALLOW, result.getAdmissionResult());
        assertEquals(DeviceStatus.ACTIVATING, result.getDeviceStatus());
    }

    @Test
    void goodCertificateAndActive_shouldAllow() {
        givenTbox(2);
        givenBinding();
        givenCertificate(CertificateStatus.VALID);

        DeviceAdmission result = deviceAdmissionDomainService.decide(command());

        assertEquals(DeviceAdmissionResult.ALLOW, result.getAdmissionResult());
        assertEquals(DeviceStatus.ACTIVE, result.getDeviceStatus());
    }

    @Test
    void revokedCertificate_shouldDeny() {
        givenTbox(1);
        givenBinding();
        givenCertificate(CertificateStatus.REVOKED);

        DeviceAdmission result = deviceAdmissionDomainService.decide(command());

        assertEquals(DeviceAdmissionResult.DENY, result.getAdmissionResult());
        assertEquals(AdmissionReason.CERT_REVOKED, result.getReason());
        assertEquals(CertificateStatus.REVOKED, result.getCertStatus());
    }

    @Test
    void expiredCertificate_shouldDeny() {
        givenTbox(1);
        givenBinding();
        when(certificateVerificationRepository.verify(any(), any())).thenReturn(new CertificateVerification(
                CertificateStatus.VALID, DECISION_AT.minus(2, ChronoUnit.DAYS), DECISION_AT.minus(1, ChronoUnit.DAYS),
                "OCSP", DECISION_AT));

        DeviceAdmission result = deviceAdmissionDomainService.decide(command());

        assertEquals(DeviceAdmissionResult.DENY, result.getAdmissionResult());
        assertEquals(AdmissionReason.CERT_EXPIRED, result.getReason());
        assertEquals(CertificateStatus.EXPIRED, result.getCertStatus());
    }

    @Test
    void unknownCertificateStatus_shouldFailClosed() {
        givenTbox(1);
        givenBinding();
        givenCertificate(CertificateStatus.UNKNOWN);

        DeviceAdmission result = deviceAdmissionDomainService.decide(command());

        assertEquals(DeviceAdmissionResult.DENY, result.getAdmissionResult());
        assertEquals(AdmissionReason.DEPENDENCY_UNAVAILABLE, result.getReason());
        assertEquals(CertificateStatus.UNKNOWN, result.getCertStatus());
    }

    @Test
    void pkiDependencyUnavailable_shouldFailClosedWithCause() {
        givenTbox(1);
        givenBinding();
        when(certificateVerificationRepository.verify(any(), any())).thenThrow(
                new CertificateDependencyUnavailableException(
                        CertificateDependencyUnavailableException.CATEGORY_DNS, "dns", new RuntimeException("boom")));

        DeviceAdmission result = deviceAdmissionDomainService.decide(command());

        assertEquals(DeviceAdmissionResult.DENY, result.getAdmissionResult());
        assertEquals(AdmissionReason.DEPENDENCY_UNAVAILABLE, result.getReason());
        assertEquals(CertificateStatus.UNKNOWN, result.getCertStatus());
        assertEquals(CertificateDependencyUnavailableException.CATEGORY_DNS, result.getDependencyOutcome());
    }

    @Test
    void deviceBlocked_shouldDenyAndNotBeOverridden() {
        givenTbox(4);
        givenBinding();
        givenCertificate(CertificateStatus.VALID);

        DeviceAdmission result = deviceAdmissionDomainService.decide(command());

        assertEquals(DeviceAdmissionResult.DENY, result.getAdmissionResult());
        assertEquals(AdmissionReason.DEVICE_BLOCKED, result.getReason());
        assertEquals(DeviceStatus.BLOCKED, result.getDeviceStatus());
        assertEquals(CertificateStatus.VALID, result.getCertStatus());
    }

    @Test
    void deviceRetired_shouldDeny() {
        givenTbox(3);
        givenBinding();
        givenCertificate(CertificateStatus.VALID);

        DeviceAdmission result = deviceAdmissionDomainService.decide(command());

        assertEquals(DeviceAdmissionResult.DENY, result.getAdmissionResult());
        assertEquals(AdmissionReason.DEVICE_RETIRED, result.getReason());
        assertEquals(DeviceStatus.RETIRED, result.getDeviceStatus());
    }

    @Test
    void invalidDeviceStatus_shouldFailClosed() {
        givenTbox(null);
        givenBinding();
        givenCertificate(CertificateStatus.VALID);

        DeviceAdmission result = deviceAdmissionDomainService.decide(command());

        assertEquals(DeviceAdmissionResult.DENY, result.getAdmissionResult());
        assertEquals(AdmissionReason.DEPENDENCY_UNAVAILABLE, result.getReason());
        assertEquals("DEVICE_STATUS_INVALID", result.getDependencyOutcome());
    }

    @Test
    void updatedAtShouldBeMaxOfSources() {
        givenTbox(1);
        Instant bindingTime = DECISION_AT.minus(2, ChronoUnit.HOURS);
        when(deviceAdmissionRepository.findByHsm(UID)).thenReturn(Optional.of(
                new DeviceAdmissionRepository.TboxInfo(1L, SN, UID, 1, DECISION_AT)));
        when(deviceAdmissionRepository.findActiveBindingsBySn(SN)).thenReturn(List.of(
                new DeviceAdmissionRepository.BindingInfo(VIN, 7L, bindingTime)));
        Instant checkedAt = DECISION_AT.minus(30, ChronoUnit.MINUTES);
        when(certificateVerificationRepository.verify(any(), any())).thenReturn(new CertificateVerification(
                CertificateStatus.VALID, DECISION_AT.minus(1, ChronoUnit.DAYS), DECISION_AT.plus(1, ChronoUnit.DAYS),
                "OCSP", checkedAt));

        DeviceAdmission result = deviceAdmissionDomainService.decide(command());

        assertEquals(DECISION_AT, result.getUpdatedAt());
        assertEquals("OCSP", result.getRevocationSource());
    }
}
