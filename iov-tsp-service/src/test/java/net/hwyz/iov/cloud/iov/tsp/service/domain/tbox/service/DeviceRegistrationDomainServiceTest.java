package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.service;

import net.hwyz.iov.cloud.iov.tsp.api.vo.event.TboxV2cKeyProvisionedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxRegistrationEventRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleTboxProjectionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxRegistrationEventRecord;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxPo;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private static final Instant OCCURRED_AT = Instant.parse("2026-10-10T08:00:00Z");

    @Mock
    private TboxMapper tboxMapper;

    @Mock
    private VehicleTboxProjectionRepository vehicleTboxProjectionRepository;

    @Mock
    private TboxRegistrationEventRepository tboxRegistrationEventRepository;

    @InjectMocks
    private DeviceRegistrationDomainService deviceRegistrationDomainService;

    @BeforeEach
    void setUp() {
        when(tboxRegistrationEventRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        when(tboxRegistrationEventRepository.findByEventTypeAndRequestId(anyString(), anyString()))
                .thenReturn(Optional.empty());
    }

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
        return TboxPo.builder().sn(SN).hsm(HSM).deviceStatus(status.getCode()).build();
    }

    private VehicleTbox activeBinding(String sn) {
        return VehicleTbox.builder().vin("VIN-1").sn(sn).deviceCategory("TBOX").bindState("ACTIVE").build();
    }

    // ---------- recordSuccessfulLogin ----------

    @Test
    void recordSuccessfulLogin_ShouldDelegateWithStatusCodesAndRecordConsumed() {
        LocalDateTime ts = LocalDateTime.of(2026, 10, 10, 8, 0);
        when(tboxMapper.recordLogin(eq(HSM), eq(ts), any(), any(), any())).thenReturn(1);

        deviceRegistrationDomainService.recordSuccessfulLogin(EVENT_ID, HSM, SN, ts);

        verify(tboxMapper).recordLogin(HSM, ts,
                DeviceStatus.PRE_ACTIVE.getCode(),
                DeviceStatus.ACTIVATING.getCode(),
                DeviceStatus.ACTIVE.getCode());
        ArgumentCaptor<TboxRegistrationEventRecord> captor = ArgumentCaptor.forClass(TboxRegistrationEventRecord.class);
        verify(tboxRegistrationEventRepository).save(captor.capture());
        assertTrue(DeviceRegistrationDomainService.EVENT_TYPE_DEVICE_ONLINE.equals(captor.getValue().eventType()));
    }

    @Test
    void recordSuccessfulLogin_BlankHsmUid_ShouldSkip() {
        deviceRegistrationDomainService.recordSuccessfulLogin(EVENT_ID, "  ", SN, LocalDateTime.now());

        verify(tboxMapper, never()).recordLogin(any(), any(), any(), any(), any());
        verify(tboxRegistrationEventRepository, never()).save(any());
    }

    @Test
    void recordSuccessfulLogin_DuplicateEventId_ShouldSkip() {
        when(tboxRegistrationEventRepository.findByEventId(EVENT_ID))
                .thenReturn(Optional.of(new TboxRegistrationEventRecord(
                        EVENT_ID, null, SN,
                        DeviceRegistrationDomainService.EVENT_TYPE_DEVICE_ONLINE,
                        null, null, "SUCCEEDED", null)));

        deviceRegistrationDomainService.recordSuccessfulLogin(EVENT_ID, HSM, SN, LocalDateTime.now());

        verify(tboxMapper, never()).recordLogin(any(), any(), any(), any(), any());
    }

    @Test
    void recordSuccessfulLogin_NullTs_ShouldFallbackToNow() {
        deviceRegistrationDomainService.recordSuccessfulLogin(null, HSM, SN, null);

        verify(tboxMapper).recordLogin(eq(HSM), any(LocalDateTime.class),
                eq(DeviceStatus.PRE_ACTIVE.getCode()),
                eq(DeviceStatus.ACTIVATING.getCode()),
                eq(DeviceStatus.ACTIVE.getCode()));
    }

    // ---------- completeActivation：契约校验 ----------

    @Test
    void completeActivation_ResultNotSucceeded_ShouldReject() {
        TboxV2cKeyProvisionedEvent event = validEvent();
        event.setResult("FAILED");

        assertFalse(deviceRegistrationDomainService.completeActivation(event));
        verify(tboxMapper, never()).completeActivation(any(), any(), any(), any());
    }

    @Test
    void completeActivation_WrongBusinessDomain_ShouldReject() {
        TboxV2cKeyProvisionedEvent event = validEvent();
        event.setBusinessDomain("OTHER");

        assertFalse(deviceRegistrationDomainService.completeActivation(event));
    }

    @Test
    void completeActivation_IncompletePurposeSet_ShouldReject() {
        TboxV2cKeyProvisionedEvent event = validEvent();
        event.setKeys(Collections.singletonList(
                key(TboxV2cKeyProvisionedEvent.PURPOSE_UPLINK_AEAD, "KID-UP", 1L)));

        assertFalse(deviceRegistrationDomainService.completeActivation(event));
    }

    @Test
    void completeActivation_DuplicatePurpose_ShouldReject() {
        TboxV2cKeyProvisionedEvent event = validEvent();
        event.setKeys(Arrays.asList(
                key(TboxV2cKeyProvisionedEvent.PURPOSE_UPLINK_AEAD, "KID-UP", 1L),
                key(TboxV2cKeyProvisionedEvent.PURPOSE_UPLINK_AEAD, "KID-UP2", 1L)));

        assertFalse(deviceRegistrationDomainService.completeActivation(event));
    }

    @Test
    void completeActivation_BlankKeyId_ShouldReject() {
        TboxV2cKeyProvisionedEvent event = validEvent();
        event.setKeys(Arrays.asList(
                key(TboxV2cKeyProvisionedEvent.PURPOSE_UPLINK_AEAD, "", 1L),
                key(TboxV2cKeyProvisionedEvent.PURPOSE_DOWNLINK_AEAD, "KID-DOWN", 1L)));

        assertFalse(deviceRegistrationDomainService.completeActivation(event));
    }

    @Test
    void completeActivation_NonPositiveVersion_ShouldReject() {
        TboxV2cKeyProvisionedEvent event = validEvent();
        event.setKeys(Arrays.asList(
                key(TboxV2cKeyProvisionedEvent.PURPOSE_UPLINK_AEAD, "KID-UP", 0L),
                key(TboxV2cKeyProvisionedEvent.PURPOSE_DOWNLINK_AEAD, "KID-DOWN", 1L)));

        assertFalse(deviceRegistrationDomainService.completeActivation(event));
    }

    // ---------- completeActivation：幂等 ----------

    @Test
    void completeActivation_EventIdAlreadyConsumed_ShouldReturnTrueWithoutLookup() {
        when(tboxRegistrationEventRepository.findByEventId(EVENT_ID))
                .thenReturn(Optional.of(new TboxRegistrationEventRecord(
                        EVENT_ID, REQUEST_ID, SN,
                        DeviceRegistrationDomainService.EVENT_TYPE_V2C_KEY_PROVISIONED,
                        OCCURRED_AT, null, "SUCCEEDED", null)));

        assertTrue(deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivation(any(), any(), any(), any());
        verify(tboxMapper, never()).selectBySn(any());
    }

    @Test
    void completeActivation_SameRequestIdIdempotent_ShouldReturnTrue() {
        when(tboxRegistrationEventRepository.findByEventTypeAndRequestId(
                DeviceRegistrationDomainService.EVENT_TYPE_V2C_KEY_PROVISIONED, REQUEST_ID))
                .thenReturn(Optional.of(new TboxRegistrationEventRecord(
                        "EVT-OLD", REQUEST_ID, SN,
                        DeviceRegistrationDomainService.EVENT_TYPE_V2C_KEY_PROVISIONED,
                        OCCURRED_AT, null, "SUCCEEDED", null)));

        assertTrue(deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivation(any(), any(), any(), any());
    }

    @Test
    void completeActivation_SameRequestIdDifferentDigest_ShouldReject() {
        when(tboxRegistrationEventRepository.findByEventTypeAndRequestId(
                DeviceRegistrationDomainService.EVENT_TYPE_V2C_KEY_PROVISIONED, REQUEST_ID))
                .thenReturn(Optional.of(new TboxRegistrationEventRecord(
                        "EVT-OLD", REQUEST_ID, SN,
                        DeviceRegistrationDomainService.EVENT_TYPE_V2C_KEY_PROVISIONED,
                        OCCURRED_AT, null, "SUCCEEDED", "deadbeef")));

        assertFalse(deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivation(any(), any(), any(), any());
    }

    // ---------- completeActivation：设备/绑定/状态 ----------

    @Test
    void completeActivation_DeviceNotFound_ShouldReject() {
        when(tboxMapper.selectBySn(SN)).thenReturn(null);

        assertFalse(deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivation(any(), any(), any(), any());
    }

    @Test
    void completeActivation_BindingMissing_ShouldReject() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.ACTIVATING));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN)).thenReturn(Collections.emptyList());

        assertFalse(deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivation(any(), any(), any(), any());
    }

    @Test
    void completeActivation_BindingConflict_ShouldReject() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.ACTIVATING));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN))
                .thenReturn(Arrays.asList(activeBinding(SN), activeBinding(SN)));

        assertFalse(deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivation(any(), any(), any(), any());
    }

    @Test
    void completeActivation_Blocked_ShouldFailClosed() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.BLOCKED));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN))
                .thenReturn(Collections.singletonList(activeBinding(SN)));

        assertFalse(deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivation(any(), any(), any(), any());
    }

    @Test
    void completeActivation_Retired_ShouldFailClosed() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.RETIRED));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN))
                .thenReturn(Collections.singletonList(activeBinding(SN)));

        assertFalse(deviceRegistrationDomainService.completeActivation(validEvent()));
    }

    @Test
    void completeActivation_PreActiveOutOfOrder_ShouldNotSkipLevel() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.PRE_ACTIVE));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN))
                .thenReturn(Collections.singletonList(activeBinding(SN)));

        assertFalse(deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivation(any(), any(), any(), any());
        verify(tboxRegistrationEventRepository, never()).save(any());
    }

    @Test
    void completeActivation_ActiveIdempotent_ShouldReturnTrueWithoutUpdate() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.ACTIVE));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN))
                .thenReturn(Collections.singletonList(activeBinding(SN)));

        assertTrue(deviceRegistrationDomainService.completeActivation(validEvent()));
        verify(tboxMapper, never()).completeActivation(any(), any(), any(), any());
        verify(tboxRegistrationEventRepository).save(any(TboxRegistrationEventRecord.class));
    }

    @Test
    void completeActivation_Activating_ShouldUpdateAndRecord() {
        when(tboxMapper.selectBySn(SN)).thenReturn(tbox(DeviceStatus.ACTIVATING));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN))
                .thenReturn(Collections.singletonList(activeBinding(SN)));
        when(tboxMapper.completeActivation(eq(SN), any(LocalDateTime.class),
                eq(DeviceStatus.ACTIVATING.getCode()), eq(DeviceStatus.ACTIVE.getCode())))
                .thenReturn(1);

        LocalDateTime expectedActivateTime = LocalDateTime.ofInstant(OCCURRED_AT, ZoneId.systemDefault());
        assertTrue(deviceRegistrationDomainService.completeActivation(validEvent()));

        verify(tboxMapper).completeActivation(SN, expectedActivateTime,
                DeviceStatus.ACTIVATING.getCode(), DeviceStatus.ACTIVE.getCode());
        verify(tboxRegistrationEventRepository).save(any(TboxRegistrationEventRecord.class));
    }
}
