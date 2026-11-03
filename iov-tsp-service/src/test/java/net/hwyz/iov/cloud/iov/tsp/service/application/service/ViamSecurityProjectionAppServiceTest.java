package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import net.hwyz.iov.cloud.iov.tsp.api.external.ViamOnboardingClient;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Tbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspViamProjectionProperties;
import net.hwyz.iov.cloud.sec.viam.api.constant.AccessInvalidationReason;
import net.hwyz.iov.cloud.sec.viam.api.constant.AccessInvalidationType;
import net.hwyz.iov.cloud.sec.viam.api.event.AccessInvalidatedEvent;
import net.hwyz.iov.cloud.sec.viam.api.event.DeviceLoginSucceededEvent;
import net.hwyz.iov.cloud.sec.viam.api.event.DeviceOnboardingChangedEvent;
import net.hwyz.iov.cloud.sec.viam.api.vo.DeviceOnboardingVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VIAM 安全投影应用服务单测（IOV-TSP-DSN-CR-016）
 * <p>
 * 覆盖：成功登录事实单调更新、快照应用（state 映射/首次登录以 VIAM 为准）、
 * 快照失败 STALE、未知状态 PARTIAL、资产未导入 RETRYABLE 路径（不静默丢弃）、
 * 不按失效事件 reason 推断状态（仅触发快照）。
 * </p>
 */
class ViamSecurityProjectionAppServiceTest {

    private TboxRepository tboxRepository;
    private ViamOnboardingClient viamOnboardingClient;
    private TspViamProjectionProperties properties;
    private ViamSecurityProjectionAppService appService;

    private static final long TBOX_ID = 42L;
    private static final String SN = "TBOX-TEST-001";
    private static final String HSM = "HSM-UID-001";
    private static final String PRINCIPAL = "PRINCIPAL-001";

    @BeforeEach
    void setUp() {
        tboxRepository = mock(TboxRepository.class);
        viamOnboardingClient = mock(ViamOnboardingClient.class);
        properties = new TspViamProjectionProperties();
        appService = new ViamSecurityProjectionAppService(tboxRepository, viamOnboardingClient, properties);
        when(tboxRepository.getByHsm(HSM)).thenReturn(tbox(SN, HSM));
        when(tboxRepository.getBySn(SN)).thenReturn(tbox(SN, HSM));
    }

    private Tbox tbox(String sn, String hsm) {
        return Tbox.builder().id(TBOX_ID).sn(sn).hsm(hsm).build();
    }

    @Test
    void loginSucceeded_ShouldUpdateLastLoginFact_AsPartial() {
        Instant occurredAt = Instant.parse("2026-10-10T08:00:00Z");
        DeviceLoginSucceededEvent event = new DeviceLoginSucceededEvent(
                "evt-1", PRINCIPAL, HSM, "VIN001", "ad-1", "sess-1", 1L,
                "AB12", occurredAt, "VAGW_RECEIVED");

        appService.handleLoginSucceeded(event);

        verify(tboxRepository).applyViamLoginFact(eq(TBOX_ID),
                eq(LocalDateTime.ofInstant(occurredAt, ZoneOffset.UTC)),
                any(LocalDateTime.class), eq(ViamSecurityProjectionAppService.SYNC_STATUS_PARTIAL));
    }

    @Test
    void loginSucceeded_AssetNotImported_ShouldThrowForRetryable() {
        when(tboxRepository.getByHsm("HSM-UNKNOWN")).thenReturn(null);
        DeviceLoginSucceededEvent event = new DeviceLoginSucceededEvent(
                "evt-2", PRINCIPAL, "HSM-UNKNOWN", "VIN001", "ad-1", "sess-1", 1L,
                "AB12", Instant.parse("2026-10-10T08:00:00Z"), "VAGW_RECEIVED");

        assertThrows(ViamSecurityProjectionAppService.TboxNotFoundProjectionException.class,
                () -> appService.handleLoginSucceeded(event));
        verify(tboxRepository, never()).applyViamLoginFact(any(), any(), any(), any());
    }

    @Test
    void onboardingChanged_ShouldTriggerSnapshotRefresh_AndApplySynced() {
        Instant first = Instant.parse("2026-10-10T08:00:00Z");
        Instant last = Instant.parse("2026-10-11T08:00:00Z");
        DeviceOnboardingChangedEvent event = new DeviceOnboardingChangedEvent(
                "evt-3", PRINCIPAL, HSM, "VIN001", SN, "ACTIVE", first, last,
                Instant.parse("2026-10-11T09:00:00Z"));
        DeviceOnboardingVo snapshot = DeviceOnboardingVo.builder()
                .principalId(PRINCIPAL).hsmUid(HSM).vin("VIN001")
                .state("ACTIVE").firstLoginTime(first).lastLoginTime(last)
                .legacyActiveTime(Instant.parse("2026-10-10T10:00:00Z"))
                .build();
        when(viamOnboardingClient.getOnboarding(PRINCIPAL)).thenReturn(snapshot);

        appService.handleOnboardingChanged(event);

        // state=ACTIVE → 码 2；first/last 以 VIAM 快照为准；activate_time 不写（V-GAP-3）
        verify(tboxRepository).applyViamSnapshot(eq(TBOX_ID), eq(2),
                eq(LocalDateTime.ofInstant(first, ZoneOffset.UTC)),
                eq(LocalDateTime.ofInstant(last, ZoneOffset.UTC)),
                eq(null), any(LocalDateTime.class),
                eq(ViamSecurityProjectionAppService.SYNC_STATUS_SYNCED));
    }

    @Test
    void onboardingChanged_SnapshotUnavailable_ShouldMarkStale_NotFakeState() {
        DeviceOnboardingChangedEvent event = new DeviceOnboardingChangedEvent(
                "evt-4", PRINCIPAL, HSM, "VIN001", SN, "ACTIVATING", null, null,
                Instant.parse("2026-10-11T09:00:00Z"));
        when(viamOnboardingClient.getOnboarding(PRINCIPAL))
                .thenThrow(new RuntimeException("VIAM unavailable"));

        appService.handleOnboardingChanged(event);

        // fail-closed：保留最后已知，标记 STALE，不按事件 state 伪装
        verify(tboxRepository).applyViamSnapshot(eq(TBOX_ID), eq(null), eq(null), eq(null),
                eq(null), any(LocalDateTime.class),
                eq(ViamSecurityProjectionAppService.SYNC_STATUS_STALE));
    }

    @Test
    void onboardingChanged_UnknownState_ShouldMarkPartial_KeepLocalDeviceStatus() {
        DeviceOnboardingChangedEvent event = new DeviceOnboardingChangedEvent(
                "evt-5", PRINCIPAL, HSM, "VIN001", SN, "UNKNOWN_STATE", null, null,
                Instant.parse("2026-10-11T09:00:00Z"));
        DeviceOnboardingVo snapshot = DeviceOnboardingVo.builder()
                .principalId(PRINCIPAL).hsmUid(HSM).state("UNKNOWN_STATE").build();
        when(viamOnboardingClient.getOnboarding(PRINCIPAL)).thenReturn(snapshot);

        appService.handleOnboardingChanged(event);

        verify(tboxRepository).applyViamSnapshot(eq(TBOX_ID), eq(null), eq(null), eq(null),
                eq(null), any(LocalDateTime.class),
                eq(ViamSecurityProjectionAppService.SYNC_STATUS_PARTIAL));
    }

    @Test
    void accessInvalidated_ShouldTriggerSnapshotOnly_NotInferBlockedFromReason() {
        AccessInvalidatedEvent event = AccessInvalidatedEvent.builder()
                .eventId("evt-6").principalId(PRINCIPAL).hsmUid(HSM).vin("VIN001").deviceSn(SN)
                .invalidationType(AccessInvalidationType.PRINCIPAL_REVOCATION)
                .reason(AccessInvalidationReason.BLOCK)
                .securityVersion(10L).effectiveAt(Instant.now()).effectiveBy(Instant.now().plusSeconds(30))
                .build();
        DeviceOnboardingVo snapshot = DeviceOnboardingVo.builder()
                .principalId(PRINCIPAL).hsmUid(HSM).vin("VIN001")
                .state("BLOCKED").build();
        when(viamOnboardingClient.getOnboarding(PRINCIPAL)).thenReturn(snapshot);

        appService.handleAccessInvalidated(event);

        // 不按 reason=BLOCK 直接推断：以 VIAM 快照 state 为准 → BLOCKED 码 4
        verify(tboxRepository).applyViamSnapshot(eq(TBOX_ID), eq(4),
                eq(null), eq(null), eq(null), any(LocalDateTime.class),
                eq(ViamSecurityProjectionAppService.SYNC_STATUS_SYNCED));
    }

    @Test
    void v2cKeyProvisioned_ShouldNotWriteActivateTime() {
        net.hwyz.iov.cloud.sec.viam.api.event.BusinessKeyChangedEvent event =
                net.hwyz.iov.cloud.sec.viam.api.event.BusinessKeyChangedEvent.builder()
                        .eventId("evt-7").deviceSn(SN).businessDomain("V2C_COMM")
                        .changeType("PROVISIONED").requestId("req-1")
                        .occurredAt(Instant.parse("2026-10-10T10:00:00Z"))
                        .build();

        appService.handleV2cKeyProvisioned(event);

        // V-GAP-2/3：不推导 ACTIVE、不把 occurredAt 填进 activate_time（仅里程碑触发，快照由其它事件驱动）
        verify(tboxRepository, never()).applyViamSnapshot(any(), any(), any(), any(), any(), any(), any());
        verify(tboxRepository, never()).applyViamLoginFact(any(), any(), any(), any());
    }
}
