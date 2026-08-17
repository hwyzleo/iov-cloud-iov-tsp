package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ResolveReason;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ResolveStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.vehicleAccessIdentity.model.VehicleAccessIdentityResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.vehicleAccessIdentity.service.VehicleAccessIdentityDomainService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleAccessIdentityAppServiceTest {

    private static final String VIN = "LSVAA2180C2000001";
    private static final String HSM = "HSM-UID-0001";

    @Mock
    private VehicleAccessIdentityDomainService vehicleAccessIdentityDomainService;

    @InjectMocks
    private VehicleAccessIdentityAppService vehicleAccessIdentityAppService;

    @Test
    void resolveByVin_ShouldReturnMappedResult() {
        VehicleAccessIdentityResult domainResult = VehicleAccessIdentityResult.builder()
                .vin(VIN)
                .status(ResolveStatus.RESOLVED)
                .reason(ResolveReason.RESOLVED)
                .hsmUid(HSM)
                .tboxSn("SN-001")
                .bindingVersion(1L)
                .build();

        when(vehicleAccessIdentityDomainService.resolveByVin(VIN)).thenReturn(domainResult);

        net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.VehicleAccessIdentityResult result =
                vehicleAccessIdentityAppService.resolveByVin(VIN);

        assertNotNull(result);
        assertEquals(VIN, result.getVin());
        assertEquals("RESOLVED", result.getStatus());
        assertEquals("RESOLVED", result.getReason());
        assertEquals(HSM, result.getHsmUid());
        assertEquals("SN-001", result.getTboxSn());
        assertEquals(1L, result.getBindingVersion());
    }

    @Test
    void resolveByVin_Unresolved_ShouldMapStatusAndReason() {
        VehicleAccessIdentityResult domainResult = VehicleAccessIdentityResult.builder()
                .vin(VIN)
                .status(ResolveStatus.UNRESOLVED)
                .reason(ResolveReason.BINDING_CONFLICT)
                .build();

        when(vehicleAccessIdentityDomainService.resolveByVin(VIN)).thenReturn(domainResult);

        net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.VehicleAccessIdentityResult result =
                vehicleAccessIdentityAppService.resolveByVin(VIN);

        assertEquals("UNRESOLVED", result.getStatus());
        assertEquals("BINDING_CONFLICT", result.getReason());
        assertNull(result.getHsmUid());
    }
}
