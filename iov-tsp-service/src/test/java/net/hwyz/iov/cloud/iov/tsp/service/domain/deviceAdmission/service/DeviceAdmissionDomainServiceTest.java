package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.service;

import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.*;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.DeviceAdmissionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.DeviceAdmissionLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.CertificateVerificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceAdmissionDomainServiceTest {

    @Mock
    private DeviceAdmissionRepository deviceAdmissionRepository;

    @Mock
    private DeviceAdmissionLogRepository deviceAdmissionLogRepository;

    @Mock
    private CertificateVerificationRepository certificateVerificationRepository;

    @InjectMocks
    private DeviceAdmissionDomainService deviceAdmissionDomainService;

    @Test
    void checkAdmission_AllChecksPass_ShouldReturnAllow() {
        String hsm = "test-hsm-123";
        String vin = "test-vin-456";

        when(deviceAdmissionRepository.findByHsm(hsm))
            .thenReturn(Optional.of(new DeviceAdmissionRepository.TboxInfo(vin, hsm, 1)));
        when(certificateVerificationRepository.checkRevocation(hsm)).thenReturn(false);

        DeviceAdmission result = deviceAdmissionDomainService.checkAdmission(hsm);

        assertEquals(DeviceAdmissionResult.ALLOW, result.getAdmissionResult());
        assertEquals(CheckStatus.PASS, result.getHsmCheckResult().getStatus());
        assertEquals(CheckStatus.PASS, result.getPkiCheckResult().getStatus());
        assertEquals(CheckStatus.PASS, result.getDeviceStatusCheckResult().getStatus());
        verify(deviceAdmissionLogRepository).save(any(DeviceAdmission.class));
    }

    @Test
    void checkAdmission_HsmBindingNotFound_ShouldReturnDeny() {
        String hsm = "test-hsm-123";

        when(deviceAdmissionRepository.findByHsm(hsm))
            .thenReturn(Optional.empty());

        DeviceAdmission result = deviceAdmissionDomainService.checkAdmission(hsm);

        assertEquals(DeviceAdmissionResult.DENY, result.getAdmissionResult());
        assertEquals(CheckStatus.FAIL, result.getHsmCheckResult().getStatus());
        verify(deviceAdmissionLogRepository).save(any(DeviceAdmission.class));
    }

    @Test
    void checkAdmission_PkiRevoked_ShouldReturnDeny() {
        String hsm = "test-hsm-123";
        String vin = "test-vin-456";

        when(deviceAdmissionRepository.findByHsm(hsm))
            .thenReturn(Optional.of(new DeviceAdmissionRepository.TboxInfo(vin, hsm, 1)));
        when(certificateVerificationRepository.checkRevocation(hsm)).thenReturn(true);

        DeviceAdmission result = deviceAdmissionDomainService.checkAdmission(hsm);

        assertEquals(DeviceAdmissionResult.DENY, result.getAdmissionResult());
        assertEquals(CheckStatus.FAIL, result.getPkiCheckResult().getStatus());
        verify(deviceAdmissionLogRepository).save(any(DeviceAdmission.class));
    }

    @Test
    void checkAdmission_DeviceStatusInactive_ShouldReturnDeny() {
        String hsm = "test-hsm-123";
        String vin = "test-vin-456";

        when(deviceAdmissionRepository.findByHsm(hsm))
            .thenReturn(Optional.of(new DeviceAdmissionRepository.TboxInfo(vin, hsm, 2)));
        when(certificateVerificationRepository.checkRevocation(hsm)).thenReturn(false);

        DeviceAdmission result = deviceAdmissionDomainService.checkAdmission(hsm);

        assertEquals(DeviceAdmissionResult.DENY, result.getAdmissionResult());
        assertEquals(CheckStatus.FAIL, result.getDeviceStatusCheckResult().getStatus());
        verify(deviceAdmissionLogRepository).save(any(DeviceAdmission.class));
    }

    @Test
    void checkAdmission_PkiServiceUnavailable_ShouldReturnAllow() {
        String hsm = "test-hsm-123";
        String vin = "test-vin-456";

        when(deviceAdmissionRepository.findByHsm(hsm))
            .thenReturn(Optional.of(new DeviceAdmissionRepository.TboxInfo(vin, hsm, 1)));
        when(certificateVerificationRepository.checkRevocation(hsm)).thenThrow(new RuntimeException("PKI服务不可用"));

        DeviceAdmission result = deviceAdmissionDomainService.checkAdmission(hsm);

        assertEquals(DeviceAdmissionResult.ALLOW, result.getAdmissionResult());
        assertEquals(CheckStatus.PASS, result.getPkiCheckResult().getStatus());
        verify(deviceAdmissionLogRepository).save(any(DeviceAdmission.class));
    }
}
