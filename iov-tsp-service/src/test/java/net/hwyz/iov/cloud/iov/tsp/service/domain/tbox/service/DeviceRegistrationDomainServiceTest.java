package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.service;

import net.hwyz.iov.cloud.iov.tsp.api.vo.event.TboxV2cKeyProvisionedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleTboxProjectionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.ActivationOutcome;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxPo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceRegistrationDomainServiceTest {

    private static final String HSM = "HSM-UID-0001";
    private static final String SN = "SN-0001";
    private static final String EVENT_ID = "EVT-1";
    private static final String REQUEST_ID = "REQ-1";
    private static final Instant OCCURRED_AT = Instant.parse("2026-10-10T08:00:00Z");

    @Mock
    private TboxMapper tboxMapper;
    @Mock
    private VehicleTboxProjectionRepository vehicleTboxProjectionRepository;

    @InjectMocks
    private DeviceRegistrationDomainService deviceRegistrationDomainService;

    private TboxV2cKeyProvisionedEvent.Key key(String purpose, String keyId, Long version) {
        return TboxV2cKeyProvisionedEvent.Key.builder()
                .purpose(purpose).keyId(keyId).businessKeyVersion(version).build();
    }

    private List<TboxV2cKeyProvisionedEvent.Key> completeKeys() {
        return Arrays.asList(
                key(TboxV2cKeyProvisionedEvent.PURPOSE_UPLINK_AEAD, "KID-UP", 1L),
                key(TboxV2cKeyProvisionedEvent.PURPOSE_DOWNLINK_AEAD, "KID-DOWN", 1L));
    }

    private TboxV2cKeyProvisionedEvent validEvent() {
        return TboxV2cKeyProvisionedEvent.builder()
                .eventId(EVENT_ID)
                .requestId(REQUEST_ID)
                .deviceSn(SN)
                .businessDomain(TboxV2cKeyProvisionedEvent.BUSINESS_DOMAIN_V2C_COMM)
                .keys(completeKeys())
                .result(TboxV2cKeyProvisionedEvent.RESULT_SUCCEEDED)
                .occurredAt(OCCURRED_AT)
                .build();
    }

    private TboxPo tbox(DeviceStatus status) {
        return TboxPo.builder().id(1L).sn(SN).hsm(HSM).deviceStatus(status.getCode()).build();
    }

    private VehicleTbox activeBinding(String sn) {
        return VehicleTbox.builder().vin("VIN-1").sn(sn).deviceCategory("TBOX").bindState("ACTIVE").build();
    }

    // ---------- recordSuccessfulLogin ----------

    @Test
    void recordSuccessfulLogin_ShouldUpdateByPrimaryKey() {
        when(tboxMapper.recordLoginById(eq(1L), any(LocalDateTime.class), any(), any(), any())).thenReturn(1);

        int updated = deviceRegistrationDomainService.recordSuccessfulLogin(1L, SN, EVENT_ID, OCCURRED_AT, 12L);

        assertEquals(1, updated);
        LocalDateTime expected = LocalDateTime.ofInstant(OCCURRED_AT, ZoneId.systemDefault());
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(tboxMapper).recordLoginById(eq(1L), captor.capture(),
                eq(DeviceStatus.PRE_ACTIVE.getCode()),
                eq(DeviceStatus.ACTIVATING.getCode()),
                eq(DeviceStatus.ACTIVE.getCode()));
        assertEquals(expected, captor.getValue());
    }

    @Test
    void recordSuccessfulLogin_NullTboxId_ShouldSkip() {
        assertEquals(0, deviceRegistrationDomainService.recordSuccessfulLogin(null, SN, EVENT_ID, OCCURRED_AT, 1L));
        verify(tboxMapper, never()).recordLoginById(any(), any(), any(), any(), any());
    }

    // ---------- completeActivation：契约校验 ----------

    @Test
    void completeActivation_ResultNotSucceeded_ShouldReject() {
        TboxV2cKeyProvisionedEvent event = validEvent();
        event.setResult("FAILED");
        assertEquals(ActivationOutcome.REJECTED, deviceRegistrationDomainService.completeActivation(event));
        verify(tboxMapper, never()).completeActivation(any(), any(), any(), any());
    }

    @Test
    void completeActivation_WrongBusinessDomain_ShouldReject() {
        TboxV2cKeyProvisionedEvent event = validEvent();
        event.setBusinessDomain("OTHER");
        assertEquals(ActivationOutcome.REJECTED, deviceRegistrationDomainService.completeActivation(event));
    }

    @Test
    void completeActivation_IncompletePurposeSet_ShouldReject() {
        TboxV2cKeyProvisionedEvent event = validEvent();
        event.setKeys(Collections.singletonList(
                key(TboxV2cKeyProvisionedEvent.PURPOSE_UPLINK_AEAD, "KID-UP", 1L)));
        assertEquals(ActivationOutcome.REJECTED, deviceRegistrationDomainService.completeActivation(event));
    }

    @Test
    void completeActivation_DuplicatePurpose_ShouldReject() {
        TboxV2cKeyProvisionedEvent event = validEvent();
        event.setKeys(Arrays.asList(
                key(TboxV2cKeyProvisionedEvent.PURPOSE_UPLINK_AEAD, "KID-UP", 1L),
                key(TboxV2cKeyProvisionedEvent.PURPOSE_UPLINK_AEAD, "KID-UP2", 1L)));
        assertEquals(ActivationOutcome.REJECTED, deviceRegistrationDomainService.completeActivation(event));
    }

    @Test
    void completeActivation_NonPositiveVersion_ShouldReject() {
        TboxV2cKeyProvisionedEvent event = validEvent();
        event.setKeys(Arrays.asList(
                key(TboxV2cKeyProvisionedEvent.PURPOSE_UPLINK_AEAD, "KID-UP", 0L),
                key(TboxV2cKeyProvisionedEvent.PURPOSE_DOWNLINK_AEAD, "KID-DOWN", 1L)));
        assertEquals(ActivationOutcome.REJECTED, deviceRegistrationDomainService.completeActivation(event));
    }

    // ---------- completeActivation：设备/绑定/状态 ----------

    @Test
    void completeActivation_DeviceNotFound_ShouldReject() {
        when(tboxMapper.selectBySn(SN)).thenReturn(null);
        assertEquals(ActivationOutcome.REJECTED, deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivation(any(), any(), any(), any());
    }

    @Test
    void completeActivation_BindingMissing_ShouldReject() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.ACTIVATING));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN)).thenReturn(Collections.emptyList());
        assertEquals(ActivationOutcome.REJECTED, deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivation(any(), any(), any(), any());
    }

    @Test
    void completeActivation_BindingConflict_ShouldReject() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.ACTIVATING));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN))
                .thenReturn(Arrays.asList(activeBinding(SN), activeBinding(SN)));
        assertEquals(ActivationOutcome.REJECTED, deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivation(any(), any(), any(), any());
    }

    @Test
    void completeActivation_Blocked_ShouldFailClosed() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.BLOCKED));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN))
                .thenReturn(Collections.singletonList(activeBinding(SN)));
        assertEquals(ActivationOutcome.REJECTED, deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivation(any(), any(), any(), any());
    }

    @Test
    void completeActivation_Retired_ShouldFailClosed() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.RETIRED));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN))
                .thenReturn(Collections.singletonList(activeBinding(SN)));
        assertEquals(ActivationOutcome.REJECTED, deviceRegistrationDomainService.completeActivation(validEvent()));
    }

    @Test
    void completeActivation_PreActiveOutOfOrder_ShouldNotSkipLevel() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.PRE_ACTIVE));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN))
                .thenReturn(Collections.singletonList(activeBinding(SN)));
        assertEquals(ActivationOutcome.OUT_OF_ORDER, deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivation(any(), any(), any(), any());
    }

    @Test
    void completeActivation_ActiveIdempotent_ShouldReturnIdempotent() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.ACTIVE));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN))
                .thenReturn(Collections.singletonList(activeBinding(SN)));
        assertEquals(ActivationOutcome.IDEMPOTENT_ACTIVE, deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivation(any(), any(), any(), any());
    }

    @Test
    void completeActivation_Activating_ShouldUpdateAndReturnActivated() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.ACTIVATING));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN))
                .thenReturn(Collections.singletonList(activeBinding(SN)));
        when(tboxMapper.completeActivation(eq(SN), any(LocalDateTime.class),
                eq(DeviceStatus.ACTIVATING.getCode()), eq(DeviceStatus.ACTIVE.getCode())))
                .thenReturn(1);

        assertEquals(ActivationOutcome.ACTIVATED, deviceRegistrationDomainService.completeActivation(validEvent()));

        LocalDateTime expectedActivateTime = LocalDateTime.ofInstant(OCCURRED_AT, ZoneId.systemDefault());
        verify(tboxMapper).completeActivation(SN, expectedActivateTime,
                DeviceStatus.ACTIVATING.getCode(), DeviceStatus.ACTIVE.getCode());
    }
}
