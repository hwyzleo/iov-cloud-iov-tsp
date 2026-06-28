package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.DeviceAdmissionCheckCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.DeviceAdmissionCheckResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.*;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.service.DeviceAdmissionDomainService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceAdmissionAppServiceTest {

    @Mock
    private DeviceAdmissionDomainService deviceAdmissionDomainService;

    @InjectMocks
    private DeviceAdmissionAppService deviceAdmissionAppService;

    @Test
    void checkAdmission_ShouldReturnResult() {
        DeviceAdmissionCheckCmd cmd = DeviceAdmissionCheckCmd.builder()
            .hsm("test-hsm-123")
            .vin("test-vin-456")
            .build();

        DeviceAdmission deviceAdmission = DeviceAdmission.builder()
            .vin("test-vin-456")
            .hsm("test-hsm-123")
            .admissionResult(DeviceAdmissionResult.ALLOW)
            .reason("所有检查通过")
            .hsmCheckResult(DeviceAdmission.CheckResult.builder().status(CheckStatus.PASS).message("HSM绑定检查通过").build())
            .pkiCheckResult(DeviceAdmission.CheckResult.builder().status(CheckStatus.PASS).message("PKI吊销检查通过").build())
            .deviceStatusCheckResult(DeviceAdmission.CheckResult.builder().status(CheckStatus.PASS).message("设备状态检查通过").build())
            .responseTimeMs(50L)
            .build();

        when(deviceAdmissionDomainService.checkAdmission("test-hsm-123", "test-vin-456"))
            .thenReturn(deviceAdmission);

        DeviceAdmissionCheckResult result = deviceAdmissionAppService.checkAdmission(cmd);

        assertNotNull(result);
        assertEquals("ALLOW", result.getAdmission());
    }
}
