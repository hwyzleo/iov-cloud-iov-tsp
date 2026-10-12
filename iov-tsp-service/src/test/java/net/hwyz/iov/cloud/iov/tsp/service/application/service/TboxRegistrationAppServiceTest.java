package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import net.hwyz.iov.cloud.iov.tsp.api.vo.event.TboxV2cKeyProvisionedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.command.RecordTboxOnlineCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.DeviceOnlineOutcome;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.support.TboxEventDigest;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxRegistrationEventRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleTboxProjectionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.ActivationOutcome;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxRegistrationEventRecord;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.service.DeviceRegistrationDomainService;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxPo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TboxRegistrationAppServiceTest {

    private static final String EVENT_ID = "EVT-1";
    private static final String HSM = "HSM-UID-0001";
    private static final String SN = "SN-0001";
    private static final String VIN = "LSVAA2180C2000001";
    private static final Instant OCCURRED_AT = Instant.parse("2026-10-10T08:00:00Z");

    @Mock
    private DeviceRegistrationDomainService deviceRegistrationDomainService;
    @Mock
    private TboxMapper tboxMapper;
    @Mock
    private VehicleTboxProjectionRepository vehicleTboxProjectionRepository;
    @Mock
    private TboxRegistrationEventRepository tboxRegistrationEventRepository;

    @InjectMocks
    private TboxRegistrationAppService tboxRegistrationAppService;

    private RecordTboxOnlineCmd cmd() {
        return new RecordTboxOnlineCmd(EVENT_ID, HSM, VIN, SN, "SESSION-1", 12L,
                OCCURRED_AT, "EMQX", "vagw.device.online");
    }

    private TboxPo tbox() {
        return TboxPo.builder().id(1L).sn(SN).hsm(HSM).deviceStatus(1).build();
    }

    private VehicleTbox binding() {
        return VehicleTbox.builder().vin(VIN).sn(SN).deviceCategory("TBOX").bindState("ACTIVE").build();
    }

    private void stubHappyPath() {
        when(tboxRegistrationEventRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        when(tboxMapper.selectAllByHsm(HSM)).thenReturn(Collections.singletonList(tbox()));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN))
                .thenReturn(Collections.singletonList(binding()));
        when(deviceRegistrationDomainService.recordSuccessfulLogin(anyLong(), anyString(), anyString(), any(), anyLong()))
                .thenReturn(1);
    }

    @Test
    void handleOnline_HappyPath_ShouldProcessAndPersistEvent() {
        stubHappyPath();

        assertEquals(DeviceOnlineOutcome.PROCESSED, tboxRegistrationAppService.handleOnline(cmd()));

        verify(deviceRegistrationDomainService).recordSuccessfulLogin(
                eq(1L), eq(SN), eq(EVENT_ID), eq(OCCURRED_AT), eq(12L));
        verify(tboxRegistrationEventRepository).save(any(TboxRegistrationEventRecord.class));
    }

    @Test
    void handleOnline_NullCmd_ShouldNoOp() {
        assertEquals(DeviceOnlineOutcome.PROCESSED, tboxRegistrationAppService.handleOnline(null));
        verify(tboxRegistrationEventRepository, never()).save(any());
    }

    @Test
    void handleOnline_DuplicateSameDigest_ShouldBeIdempotent() {
        String digest = TboxEventDigest.deviceOnline(cmd());
        when(tboxRegistrationEventRepository.findByEventId(EVENT_ID))
                .thenReturn(Optional.of(new TboxRegistrationEventRecord(
                        EVENT_ID, null, SN, DeviceRegistrationDomainService.EVENT_TYPE_DEVICE_ONLINE,
                        OCCURRED_AT, null, "SUCCEEDED", digest, "vagw.device.online", 12L)));

        assertEquals(DeviceOnlineOutcome.DUPLICATE, tboxRegistrationAppService.handleOnline(cmd()));
        verify(tboxRegistrationEventRepository, never()).save(any());
    }

    @Test
    void handleOnline_DuplicateLegacyNullDigest_ShouldBeIdempotent() {
        when(tboxRegistrationEventRepository.findByEventId(EVENT_ID))
                .thenReturn(Optional.of(new TboxRegistrationEventRecord(
                        EVENT_ID, null, SN, DeviceRegistrationDomainService.EVENT_TYPE_DEVICE_ONLINE,
                        OCCURRED_AT, null, "SUCCEEDED", null, null, null)));

        assertEquals(DeviceOnlineOutcome.DUPLICATE, tboxRegistrationAppService.handleOnline(cmd()));
        verify(tboxRegistrationEventRepository, never()).save(any());
    }

    @Test
    void handleOnline_SameEventIdDifferentDigest_ShouldConflict() {
        when(tboxRegistrationEventRepository.findByEventId(EVENT_ID))
                .thenReturn(Optional.of(new TboxRegistrationEventRecord(
                        EVENT_ID, null, SN, DeviceRegistrationDomainService.EVENT_TYPE_DEVICE_ONLINE,
                        OCCURRED_AT, null, "SUCCEEDED", "deadbeef", null, null)));

        assertEquals(DeviceOnlineOutcome.PERMANENT_CONTRACT_CONFLICT, tboxRegistrationAppService.handleOnline(cmd()));
        verify(tboxRegistrationEventRepository, never()).save(any());
    }

    @Test
    void handleOnline_TboxNotFound_ShouldRetry() {
        when(tboxRegistrationEventRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        when(tboxMapper.selectAllByHsm(HSM)).thenReturn(Collections.emptyList());

        assertEquals(DeviceOnlineOutcome.RETRY_TBOX_NOT_FOUND, tboxRegistrationAppService.handleOnline(cmd()));
    }

    @Test
    void handleOnline_TboxIdentityConflict_ShouldReject() {
        when(tboxRegistrationEventRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        when(tboxMapper.selectAllByHsm(HSM)).thenReturn(Arrays.asList(tbox(), tbox()));

        assertEquals(DeviceOnlineOutcome.PERMANENT_TBOX_IDENTITY_CONFLICT,
                tboxRegistrationAppService.handleOnline(cmd()));
    }

    @Test
    void handleOnline_TboxSnMismatch_ShouldReject() {
        when(tboxRegistrationEventRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        when(tboxMapper.selectAllByHsm(HSM))
                .thenReturn(Collections.singletonList(TboxPo.builder().id(1L).sn("OTHER-SN").hsm(HSM).build()));

        assertEquals(DeviceOnlineOutcome.PERMANENT_TBOX_SN_MISMATCH,
                tboxRegistrationAppService.handleOnline(cmd()));
    }

    @Test
    void handleOnline_Unbound_ShouldRetry() {
        when(tboxRegistrationEventRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        when(tboxMapper.selectAllByHsm(HSM)).thenReturn(Collections.singletonList(tbox()));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN)).thenReturn(Collections.emptyList());

        assertEquals(DeviceOnlineOutcome.RETRY_UNBOUND, tboxRegistrationAppService.handleOnline(cmd()));
    }

    @Test
    void handleOnline_BindingConflict_ShouldReject() {
        when(tboxRegistrationEventRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        when(tboxMapper.selectAllByHsm(HSM)).thenReturn(Collections.singletonList(tbox()));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN))
                .thenReturn(Arrays.asList(binding(), binding()));

        assertEquals(DeviceOnlineOutcome.PERMANENT_BINDING_CONFLICT,
                tboxRegistrationAppService.handleOnline(cmd()));
    }

    @Test
    void handleOnline_VinMismatch_ShouldReject() {
        when(tboxRegistrationEventRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        when(tboxMapper.selectAllByHsm(HSM)).thenReturn(Collections.singletonList(tbox()));
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(SN))
                .thenReturn(Collections.singletonList(VehicleTbox.builder().vin("OTHER-VIN").sn(SN).build()));

        assertEquals(DeviceOnlineOutcome.PERMANENT_VIN_MISMATCH,
                tboxRegistrationAppService.handleOnline(cmd()));
    }

    @Test
    void handleV2cProvisioned_AlreadyConsumedSameDigest_ShouldReturnTrue() {
        TboxV2cKeyProvisionedEvent event = validV2cEvent();
        when(tboxRegistrationEventRepository.findByEventId(EVENT_ID))
                .thenReturn(Optional.of(new TboxRegistrationEventRecord(
                        EVENT_ID, "REQ-1", SN, DeviceRegistrationDomainService.EVENT_TYPE_V2C_KEY_PROVISIONED,
                        OCCURRED_AT, null, "SUCCEEDED", TboxEventDigest.v2c(event), null, null)));

        assertTrue(tboxRegistrationAppService.handleV2cProvisioned(event));
        verify(deviceRegistrationDomainService, never()).completeActivation(any());
    }

    @Test
    void handleV2cProvisioned_Activated_ShouldPersistAndReturnTrue() {
        TboxV2cKeyProvisionedEvent event = validV2cEvent();
        when(tboxRegistrationEventRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        when(tboxRegistrationEventRepository.findByEventTypeAndRequestId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(deviceRegistrationDomainService.completeActivation(event)).thenReturn(ActivationOutcome.ACTIVATED);

        assertTrue(tboxRegistrationAppService.handleV2cProvisioned(event));
        verify(tboxRegistrationEventRepository).save(any(TboxRegistrationEventRecord.class));
    }

    @Test
    void handleV2cProvisioned_Rejected_ShouldNotPersistAndReturnFalse() {
        TboxV2cKeyProvisionedEvent event = validV2cEvent();
        when(tboxRegistrationEventRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        when(tboxRegistrationEventRepository.findByEventTypeAndRequestId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(deviceRegistrationDomainService.completeActivation(event)).thenReturn(ActivationOutcome.REJECTED);

        assertFalse(tboxRegistrationAppService.handleV2cProvisioned(event));
        verify(tboxRegistrationEventRepository, never()).save(any());
    }

    private TboxV2cKeyProvisionedEvent validV2cEvent() {
        List<TboxV2cKeyProvisionedEvent.Key> keys = Arrays.asList(
                TboxV2cKeyProvisionedEvent.Key.builder()
                        .purpose(TboxV2cKeyProvisionedEvent.PURPOSE_UPLINK_AEAD).keyId("K-UP").businessKeyVersion(1L).build(),
                TboxV2cKeyProvisionedEvent.Key.builder()
                        .purpose(TboxV2cKeyProvisionedEvent.PURPOSE_DOWNLINK_AEAD).keyId("K-DOWN").businessKeyVersion(1L).build());
        return TboxV2cKeyProvisionedEvent.builder()
                .eventId(EVENT_ID)
                .requestId("REQ-1")
                .deviceSn(SN)
                .businessDomain(TboxV2cKeyProvisionedEvent.BUSINESS_DOMAIN_V2C_COMM)
                .keys(keys)
                .result(TboxV2cKeyProvisionedEvent.RESULT_SUCCEEDED)
                .occurredAt(OCCURRED_AT)
                .build();
    }
}
