package net.hwyz.iov.cloud.iov.tsp.service.domain.vehicleAccessIdentity.service;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ResolveReason;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ResolveStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Tbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleTboxProjectionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.vehicleAccessIdentity.model.VehicleAccessIdentityResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleAccessIdentityDomainServiceTest {

    private static final String VIN = "LSVAA2180C2000001";
    private static final String SN_OLD = "SN-OLD-001";
    private static final String SN_NEW = "SN-NEW-002";
    private static final String HSM_OLD = "HSM-OLD-UID-0001";
    private static final String HSM_NEW = "HSM-NEW-UID-0002";

    @Mock
    private VehicleTboxProjectionRepository vehicleTboxProjectionRepository;

    @Mock
    private TboxRepository tboxRepository;

    @InjectMocks
    private VehicleAccessIdentityDomainService vehicleAccessIdentityDomainService;

    private VehicleTbox activeBinding(String sn, Long bindingVersion, String bindState) {
        return VehicleTbox.builder()
                .vin(VIN)
                .sn(sn)
                .bindingId(bindingVersion)
                .deviceCategory("TBOX")
                .bindState(bindState)
                .bindingVersion(bindingVersion)
                .lastEventTime(Instant.parse("2026-08-17T10:00:00Z"))
                .build();
    }

    private Tbox tbox(String sn, String hsm) {
        return Tbox.builder()
                .sn(sn)
                .hsm(hsm)
                .build();
    }

    @Test
    void resolveByVin_BlankVin_ShouldReturnVIN_UNKNOWN() {
        VehicleAccessIdentityResult result = vehicleAccessIdentityDomainService.resolveByVin("  ");

        assertEquals(ResolveStatus.UNRESOLVED, result.getStatus());
        assertEquals(ResolveReason.VIN_UNKNOWN, result.getReason());
        assertNull(result.getHsmUid());
    }

    @Test
    void resolveByVin_NoActiveBinding_ShouldReturnUNBOUND() {
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsByVin(VIN)).thenReturn(List.of());

        VehicleAccessIdentityResult result = vehicleAccessIdentityDomainService.resolveByVin(VIN);

        assertEquals(ResolveStatus.UNRESOLVED, result.getStatus());
        assertEquals(ResolveReason.UNBOUND, result.getReason());
        assertNull(result.getHsmUid());
    }

    @Test
    void resolveByVin_MultipleActiveBindings_ShouldReturnBINDING_CONFLICT() {
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsByVin(VIN))
                .thenReturn(List.of(activeBinding(SN_OLD, 1L, "ACTIVE"), activeBinding(SN_NEW, 2L, "ACTIVE")));

        VehicleAccessIdentityResult result = vehicleAccessIdentityDomainService.resolveByVin(VIN);

        assertEquals(ResolveStatus.UNRESOLVED, result.getStatus());
        assertEquals(ResolveReason.BINDING_CONFLICT, result.getReason());
        assertNull(result.getHsmUid());
    }

    @Test
    void resolveByVin_TboxDeviceMissing_ShouldReturnTBOX_NOT_FOUND() {
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsByVin(VIN))
                .thenReturn(List.of(activeBinding(SN_OLD, 1L, "ACTIVE")));
        when(tboxRepository.getBySn(SN_OLD)).thenReturn(null);

        VehicleAccessIdentityResult result = vehicleAccessIdentityDomainService.resolveByVin(VIN);

        assertEquals(ResolveStatus.UNRESOLVED, result.getStatus());
        assertEquals(ResolveReason.TBOX_NOT_FOUND, result.getReason());
        assertNull(result.getHsmUid());
    }

    @Test
    void resolveByVin_HsmMissing_ShouldReturnHSM_UID_MISSING() {
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsByVin(VIN))
                .thenReturn(List.of(activeBinding(SN_OLD, 1L, "ACTIVE")));
        when(tboxRepository.getBySn(SN_OLD)).thenReturn(tbox(SN_OLD, null));

        VehicleAccessIdentityResult result = vehicleAccessIdentityDomainService.resolveByVin(VIN);

        assertEquals(ResolveStatus.UNRESOLVED, result.getStatus());
        assertEquals(ResolveReason.HSM_UID_MISSING, result.getReason());
        assertNull(result.getHsmUid());
    }

    @Test
    void resolveByVin_HsmBlank_ShouldReturnHSM_UID_MISSING() {
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsByVin(VIN))
                .thenReturn(List.of(activeBinding(SN_OLD, 1L, "ACTIVE")));
        when(tboxRepository.getBySn(SN_OLD)).thenReturn(tbox(SN_OLD, " "));

        VehicleAccessIdentityResult result = vehicleAccessIdentityDomainService.resolveByVin(VIN);

        assertEquals(ResolveStatus.UNRESOLVED, result.getStatus());
        assertEquals(ResolveReason.HSM_UID_MISSING, result.getReason());
        assertNull(result.getHsmUid());
    }

    @Test
    void resolveByVin_Success_ShouldReturnRESOLVED() {
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsByVin(VIN))
                .thenReturn(List.of(activeBinding(SN_OLD, 1L, "ACTIVE")));
        when(tboxRepository.getBySn(SN_OLD)).thenReturn(tbox(SN_OLD, HSM_OLD));

        VehicleAccessIdentityResult result = vehicleAccessIdentityDomainService.resolveByVin(VIN);

        assertEquals(ResolveStatus.RESOLVED, result.getStatus());
        assertEquals(ResolveReason.RESOLVED, result.getReason());
        assertEquals(HSM_OLD, result.getHsmUid());
        assertEquals(SN_OLD, result.getTboxSn());
        assertEquals(1L, result.getBindingVersion());
        assertNotNull(result.getBindingUpdatedAt());
    }

    @Test
    void resolveByVin_RepositoryException_ShouldReturnDEPENDENCY_UNAVAILABLE() {
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsByVin(VIN))
                .thenThrow(new RuntimeException("数据库连接失败"));

        VehicleAccessIdentityResult result = vehicleAccessIdentityDomainService.resolveByVin(VIN);

        assertEquals(ResolveStatus.UNRESOLVED, result.getStatus());
        assertEquals(ResolveReason.DEPENDENCY_UNAVAILABLE, result.getReason());
        assertNull(result.getHsmUid());
    }

    @Test
    void resolveByVin_AfterReplace_ShouldReturnOnlyNewestBinding() {
        // REPLACE 后：旧绑定 INACTIVE，新绑定 ACTIVE；投影仅返回新绑定，解析应返回新 HSM 与新版本
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsByVin(VIN))
                .thenReturn(List.of(activeBinding(SN_NEW, 2L, "ACTIVE")));
        when(tboxRepository.getBySn(SN_NEW)).thenReturn(tbox(SN_NEW, HSM_NEW));

        VehicleAccessIdentityResult result = vehicleAccessIdentityDomainService.resolveByVin(VIN);

        assertEquals(ResolveStatus.RESOLVED, result.getStatus());
        assertEquals(HSM_NEW, result.getHsmUid());
        assertEquals(SN_NEW, result.getTboxSn());
        assertEquals(2L, result.getBindingVersion());
        assertNotEquals(HSM_OLD, result.getHsmUid());
    }

    @Test
    void resolveByVin_AfterUnbind_ShouldNotResolveOldIdentity() {
        // UNBIND 后：无 ACTIVE 绑定，旧身份不可再作为当前解析结果
        when(vehicleTboxProjectionRepository.listActiveTboxBindingsByVin(VIN)).thenReturn(List.of());

        VehicleAccessIdentityResult result = vehicleAccessIdentityDomainService.resolveByVin(VIN);

        assertEquals(ResolveStatus.UNRESOLVED, result.getStatus());
        assertEquals(ResolveReason.UNBOUND, result.getReason());
        assertNull(result.getHsmUid());
    }
}
