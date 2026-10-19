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
    private static final Long TBOX_ID = 1L;
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
                .contractVersion(TboxV2cKeyProvisionedEvent.CONTRACT_VERSION_V1)
                .businessDomain(TboxV2cKeyProvisionedEvent.BUSINESS_DOMAIN_V2C_COMM)
                .keys(completeKeys())
                .result(TboxV2cKeyProvisionedEvent.RESULT_SUCCEEDED)
                .occurredAt(OCCURRED_AT)
                .build();
    }

    private TboxPo tbox(DeviceStatus status) {
        return TboxPo.builder().id(TBOX_ID).sn(SN).hsm(HSM).deviceStatus(status.getCode()).build();
    }

    private VehicleTbox activeBinding(String sn) {
        return VehicleTbox.builder().vin("VIN-1").sn(sn).deviceCategory("TBOX").bindState("ACTIVE").build();
    }

    private void stubBinding(String sn) {
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(sn))
                .thenReturn(Collections.singletonList(activeBinding(sn)));
    }

    // ---------- recordSuccessfulLogin ----------

    @Test
    void recordSuccessfulLogin_ShouldUpdateByPrimaryKey() {
        when(tboxMapper.recordLoginById(eq(TBOX_ID), any(LocalDateTime.class), any(), any(), any())).thenReturn(1);

        int updated = deviceRegistrationDomainService.recordSuccessfulLogin(TBOX_ID, SN, EVENT_ID, OCCURRED_AT, 12L);

        assertEquals(1, updated);
        LocalDateTime expected = LocalDateTime.ofInstant(OCCURRED_AT, ZoneId.systemDefault());
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(tboxMapper).recordLoginById(eq(TBOX_ID), captor.capture(),
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
    void completeActivation_ResultNotSucceeded_ShouldRejectContract() {
        TboxV2cKeyProvisionedEvent event = validEvent();
        event.setResult("FAILED");
        assertEquals(ActivationOutcome.REJECTED_CONTRACT, deviceRegistrationDomainService.completeActivation(event));
        verify(tboxMapper, never()).selectBySn(anyString());
    }

    @Test
    void completeActivation_WrongBusinessDomain_ShouldRejectContract() {
        TboxV2cKeyProvisionedEvent event = validEvent();
        event.setBusinessDomain("OTHER");
        assertEquals(ActivationOutcome.REJECTED_CONTRACT, deviceRegistrationDomainService.completeActivation(event));
    }

    @Test
    void completeActivation_UnsupportedContractVersion_ShouldRejectContract() {
        TboxV2cKeyProvisionedEvent event = validEvent();
        event.setContractVersion("2");
        assertEquals(ActivationOutcome.REJECTED_CONTRACT, deviceRegistrationDomainService.completeActivation(event));
    }

    @Test
    void completeActivation_MissingContractVersion_ShouldBeCompatible() {
        TboxV2cKeyProvisionedEvent event = validEvent();
        event.setContractVersion(null);
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.ACTIVE));
        stubBinding(SN);
        assertEquals(ActivationOutcome.IDEMPOTENT_ACTIVE, deviceRegistrationDomainService.completeActivation(event));
    }

    @Test
    void completeActivation_IncompletePurposeSet_ShouldRejectContract() {
        TboxV2cKeyProvisionedEvent event = validEvent();
        event.setKeys(Collections.singletonList(
                key(TboxV2cKeyProvisionedEvent.PURPOSE_UPLINK_AEAD, "KID-UP", 1L)));
        assertEquals(ActivationOutcome.REJECTED_CONTRACT, deviceRegistrationDomainService.completeActivation(event));
    }

    @Test
    void completeActivation_DuplicatePurpose_ShouldRejectContract() {
        TboxV2cKeyProvisionedEvent event = validEvent();
        event.setKeys(Arrays.asList(
                key(TboxV2cKeyProvisionedEvent.PURPOSE_UPLINK_AEAD, "KID-UP", 1L),
                key(TboxV2cKeyProvisionedEvent.PURPOSE_UPLINK_AEAD, "KID-UP2", 1L)));
        assertEquals(ActivationOutcome.REJECTED_CONTRACT, deviceRegistrationDomainService.completeActivation(event));
    }

    @Test
    void completeActivation_NonPositiveVersion_ShouldRejectContract() {
        TboxV2cKeyProvisionedEvent event = validEvent();
        event.setKeys(Arrays.asList(
                key(TboxV2cKeyProvisionedEvent.PURPOSE_UPLINK_AEAD, "KID-UP", 0L),
                key(TboxV2cKeyProvisionedEvent.PURPOSE_DOWNLINK_AEAD, "KID-DOWN", 1L)));
        assertEquals(ActivationOutcome.REJECTED_CONTRACT, deviceRegistrationDomainService.completeActivation(event));
    }

    // ---------- completeActivation：设备/绑定/状态 ----------

    @Test
    void completeActivation_DeviceNotFound_ShouldRetryDeviceNotFound() {
        when(tboxMapper.selectBySn(SN)).thenReturn(null);
        assertEquals(ActivationOutcome.RETRY_DEVICE_NOT_FOUND,
                deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivationById(any(), any(), any(), any());
    }

    @Test
    void completeActivation_BindingMissing_ShouldRetryBindingMissing() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.ACTIVATING));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN)).thenReturn(Collections.emptyList());
        assertEquals(ActivationOutcome.RETRY_BINDING_MISSING,
                deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivationById(any(), any(), any(), any());
    }

    @Test
    void completeActivation_BindingConflict_ShouldRejectPermanently() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.ACTIVATING));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN))
                .thenReturn(Arrays.asList(activeBinding(SN), activeBinding(SN)));
        assertEquals(ActivationOutcome.REJECTED_BINDING_CONFLICT,
                deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivationById(any(), any(), any(), any());
    }

    @Test
    void completeActivation_Blocked_ShouldFailClosed() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.BLOCKED));
        stubBinding(SN);
        assertEquals(ActivationOutcome.REJECTED_DEVICE_STATE,
                deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivationById(any(), any(), any(), any());
    }

    @Test
    void completeActivation_Retired_ShouldFailClosed() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.RETIRED));
        stubBinding(SN);
        assertEquals(ActivationOutcome.REJECTED_DEVICE_STATE,
                deviceRegistrationDomainService.completeActivation(validEvent()));
    }

    @Test
    void completeActivation_PreActiveOutOfOrder_ShouldRetryOutOfOrder() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.PRE_ACTIVE));
        stubBinding(SN);
        assertEquals(ActivationOutcome.RETRY_OUT_OF_ORDER,
                deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivationById(any(), any(), any(), any());
    }

    @Test
    void completeActivation_ActiveIdempotent_ShouldReturnIdempotent() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.ACTIVE));
        stubBinding(SN);
        assertEquals(ActivationOutcome.IDEMPOTENT_ACTIVE,
                deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivationById(any(), any(), any(), any());
    }

    @Test
    void completeActivation_Activating_ShouldUpdateByResolvedId() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.ACTIVATING));
        stubBinding(SN);
        when(tboxMapper.completeActivationById(eq(TBOX_ID), any(LocalDateTime.class),
                eq(DeviceStatus.ACTIVATING.getCode()), eq(DeviceStatus.ACTIVE.getCode())))
                .thenReturn(1);

        assertEquals(ActivationOutcome.ACTIVATED, deviceRegistrationDomainService.completeActivation(validEvent()));

        LocalDateTime expectedActivateTime = LocalDateTime.ofInstant(OCCURRED_AT, ZoneId.systemDefault());
        verify(tboxMapper).completeActivationById(TBOX_ID, expectedActivateTime,
                DeviceStatus.ACTIVATING.getCode(), DeviceStatus.ACTIVE.getCode());
    }

    @Test
    void completeActivation_UpdateMissThenActive_ShouldBeIdempotent() {
        when(tboxMapper.selectBySn(SN))
                .thenReturn(tbox(DeviceStatus.ACTIVATING))
                .thenReturn(tbox(DeviceStatus.ACTIVE));
        stubBinding(SN);
        when(tboxMapper.completeActivationById(eq(TBOX_ID), any(LocalDateTime.class), any(), any())).thenReturn(0);

        assertEquals(ActivationOutcome.IDEMPOTENT_ACTIVE,
                deviceRegistrationDomainService.completeActivation(validEvent()));
    }

    @Test
    void completeActivation_UpdateMissThenStillActivating_ShouldRetryTransient() {
        when(tboxMapper.selectBySn(SN))
                .thenReturn(tbox(DeviceStatus.ACTIVATING))
                .thenReturn(tbox(DeviceStatus.ACTIVATING));
        stubBinding(SN);
        when(tboxMapper.completeActivationById(eq(TBOX_ID), any(LocalDateTime.class), any(), any())).thenReturn(0);

        assertEquals(ActivationOutcome.RETRY_TRANSIENT,
                deviceRegistrationDomainService.completeActivation(validEvent()));
    }

    @Test
    void completeActivation_UpdateMissThenRetired_ShouldRejectPermanently() {
        when(tboxMapper.selectBySn(SN))
                .thenReturn(tbox(DeviceStatus.ACTIVATING))
                .thenReturn(tbox(DeviceStatus.RETIRED));
        stubBinding(SN);
        when(tboxMapper.completeActivationById(eq(TBOX_ID), any(LocalDateTime.class), any(), any())).thenReturn(0);

        assertEquals(ActivationOutcome.REJECTED_DEVICE_STATE,
                deviceRegistrationDomainService.completeActivation(validEvent()));
    }
}
