package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import net.hwyz.iov.cloud.iov.tsp.api.vo.event.TboxDeviceOnlineEvent;
import net.hwyz.iov.cloud.iov.tsp.api.vo.event.TboxV2cKeyProvisionedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.service.DeviceRegistrationDomainService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TboxRegistrationAppServiceTest {

    private static final String EVENT_ID = "EVT-1";
    private static final String HSM = "HSM-UID-0001";
    private static final String SN = "SN-0001";
    private static final Instant TS = Instant.parse("2026-10-10T08:00:00Z");

    @Mock
    private DeviceRegistrationDomainService deviceRegistrationDomainService;

    @InjectMocks
    private TboxRegistrationAppService tboxRegistrationAppService;

    @Test
    void handleOnline_ShouldMapEventTimeAndDelegate() {
        TboxDeviceOnlineEvent event = TboxDeviceOnlineEvent.builder()
                .eventId(EVENT_ID)
                .hsmUid(HSM)
                .tboxSn(SN)
                .vin("LSVAA2180C2000001")
                .ts(TS)
                .build();

        tboxRegistrationAppService.handleOnline(event);

        LocalDateTime expected = LocalDateTime.ofInstant(TS, ZoneId.systemDefault());
        verify(deviceRegistrationDomainService).recordSuccessfulLogin(EVENT_ID, HSM, SN, expected);
    }

    @Test
    void handleOnline_NullEvent_ShouldSkip() {
        tboxRegistrationAppService.handleOnline(null);

        verify(deviceRegistrationDomainService, never()).recordSuccessfulLogin(any(), any(), any(), any());
    }

    @Test
    void handleOnline_NullTs_ShouldFallbackToNow() {
        TboxDeviceOnlineEvent event = TboxDeviceOnlineEvent.builder()
                .eventId(EVENT_ID)
                .hsmUid(HSM)
                .tboxSn(SN)
                .build();

        tboxRegistrationAppService.handleOnline(event);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(deviceRegistrationDomainService).recordSuccessfulLogin(eq(EVENT_ID), eq(HSM), eq(SN), captor.capture());
        assertNotNull(captor.getValue());
    }

    @Test
    void handleV2cProvisioned_ShouldDelegateAndReturnResult() {
        TboxV2cKeyProvisionedEvent event = TboxV2cKeyProvisionedEvent.builder()
                .requestId("REQ-1")
                .build();
        when(deviceRegistrationDomainService.completeActivation(event)).thenReturn(true);

        assertTrue(tboxRegistrationAppService.handleV2cProvisioned(event));
    }

    @Test
    void handleV2cProvisioned_DomainRejects_ShouldReturnFalse() {
        TboxV2cKeyProvisionedEvent event = TboxV2cKeyProvisionedEvent.builder()
                .requestId("REQ-1")
                .build();
        when(deviceRegistrationDomainService.completeActivation(event)).thenReturn(false);

        assertFalse(tboxRegistrationAppService.handleV2cProvisioned(event));
    }
}
