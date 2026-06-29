package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.service.DeviceActivationDomainService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TboxEventAppServiceTest {

    @Mock
    private DeviceActivationDomainService deviceActivationDomainService;

    @InjectMocks
    private TboxEventAppService tboxEventAppService;

    @Test
    void handleDeviceOnline_ShouldDelegateToDomainService() {
        String sn = "test-sn-123";
        LocalDateTime ts = LocalDateTime.now();

        tboxEventAppService.handleDeviceOnline(sn, ts);

        verify(deviceActivationDomainService).activateOnFirstOnline(sn, ts);
    }
}
