package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehiclePartBindingChangedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleCcp;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleTboxProjectionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleCcpProjectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleBindingProjectionAppServiceTest {

    @Mock
    private VehicleTboxProjectionRepository vehicleTboxProjectionRepository;

    @Mock
    private VehicleCcpProjectionRepository vehicleCcpProjectionRepository;

    @InjectMocks
    private VehicleBindingProjectionAppService projectionAppService;

    @Test
    void handleBindingChangedEvent_TboxBind_ShouldUpsertProjection() {
        VehiclePartBindingChangedEvent event = VehiclePartBindingChangedEvent.builder()
            .vin("TEST_VIN")
            .bindingId(123L)
            .partCode("PART_001")
            .sn("SN_001")
            .deviceCategory("TBOX")
            .vehicleNodeCode("NODE_001")
            .changeType("BIND")
            .occurredAt(Instant.now())
            .seq(1L)
            .build();

        when(vehicleTboxProjectionRepository.upsertByBindingId(any(VehicleTbox.class))).thenReturn(1);

        projectionAppService.handleBindingChangedEvent(event);

        verify(vehicleTboxProjectionRepository, times(1)).upsertByBindingId(any(VehicleTbox.class));
        verify(vehicleCcpProjectionRepository, never()).upsertByBindingId(any(VehicleCcp.class));
    }

    @Test
    void handleBindingChangedEvent_TboxUnbind_ShouldSetInactive() {
        VehiclePartBindingChangedEvent event = VehiclePartBindingChangedEvent.builder()
            .vin("TEST_VIN")
            .bindingId(123L)
            .deviceCategory("TBOX")
            .changeType("UNBIND")
            .occurredAt(Instant.now())
            .seq(2L)
            .build();

        VehicleTbox existing = VehicleTbox.builder()
            .id(1L)
            .vin("TEST_VIN")
            .bindingId(123L)
            .bindState("ACTIVE")
            .build();

        when(vehicleTboxProjectionRepository.getByBindingId(123L)).thenReturn(existing);
        when(vehicleTboxProjectionRepository.upsertByBindingId(any(VehicleTbox.class))).thenReturn(1);

        projectionAppService.handleBindingChangedEvent(event);

        verify(vehicleTboxProjectionRepository, times(1)).upsertByBindingId(argThat(t ->
            "INACTIVE".equals(t.getBindState())));
    }

    @Test
    void handleBindingChangedEvent_TboxReplace_ShouldUpdateBothBindings() {
        VehiclePartBindingChangedEvent event = VehiclePartBindingChangedEvent.builder()
            .vin("TEST_VIN")
            .bindingId(456L)
            .partCode("PART_002")
            .sn("SN_002")
            .deviceCategory("TBOX")
            .changeType("REPLACE")
            .replaceOfBindingId(123L)
            .occurredAt(Instant.now())
            .seq(3L)
            .build();

        VehicleTbox replaced = VehicleTbox.builder()
            .id(1L)
            .vin("TEST_VIN")
            .bindingId(123L)
            .bindState("ACTIVE")
            .build();

        when(vehicleTboxProjectionRepository.getByBindingId(123L)).thenReturn(replaced);
        when(vehicleTboxProjectionRepository.upsertByBindingId(any(VehicleTbox.class))).thenReturn(1);

        projectionAppService.handleBindingChangedEvent(event);

        verify(vehicleTboxProjectionRepository, times(2)).upsertByBindingId(any(VehicleTbox.class));
    }

    @Test
    void handleBindingChangedEvent_CcpBind_ShouldUpsertProjection() {
        VehiclePartBindingChangedEvent event = VehiclePartBindingChangedEvent.builder()
            .vin("TEST_VIN")
            .bindingId(789L)
            .partCode("PART_003")
            .sn("SN_003")
            .deviceCategory("CCP")
            .vehicleNodeCode("NODE_002")
            .changeType("BIND")
            .occurredAt(Instant.now())
            .seq(1L)
            .build();

        when(vehicleCcpProjectionRepository.upsertByBindingId(any(VehicleCcp.class))).thenReturn(1);

        projectionAppService.handleBindingChangedEvent(event);

        verify(vehicleCcpProjectionRepository, times(1)).upsertByBindingId(any(VehicleCcp.class));
        verify(vehicleTboxProjectionRepository, never()).upsertByBindingId(any(VehicleTbox.class));
    }

    @Test
    void handleBindingChangedEvent_CcpUnbind_ShouldSetInactive() {
        VehiclePartBindingChangedEvent event = VehiclePartBindingChangedEvent.builder()
            .vin("TEST_VIN")
            .bindingId(789L)
            .deviceCategory("CCP")
            .changeType("UNBIND")
            .occurredAt(Instant.now())
            .seq(2L)
            .build();

        VehicleCcp existing = VehicleCcp.builder()
            .id(1L)
            .vin("TEST_VIN")
            .bindingId(789L)
            .bindState("ACTIVE")
            .build();

        when(vehicleCcpProjectionRepository.getByBindingId(789L)).thenReturn(existing);
        when(vehicleCcpProjectionRepository.upsertByBindingId(any(VehicleCcp.class))).thenReturn(1);

        projectionAppService.handleBindingChangedEvent(event);

        verify(vehicleCcpProjectionRepository, times(1)).upsertByBindingId(argThat(c ->
            "INACTIVE".equals(c.getBindState())));
    }

    @Test
    void handleBindingChangedEvent_CcpReplace_ShouldUpdateBothBindings() {
        VehiclePartBindingChangedEvent event = VehiclePartBindingChangedEvent.builder()
            .vin("TEST_VIN")
            .bindingId(101L)
            .partCode("PART_004")
            .sn("SN_004")
            .deviceCategory("CCP")
            .changeType("REPLACE")
            .replaceOfBindingId(789L)
            .occurredAt(Instant.now())
            .seq(3L)
            .build();

        VehicleCcp replaced = VehicleCcp.builder()
            .id(1L)
            .vin("TEST_VIN")
            .bindingId(789L)
            .bindState("ACTIVE")
            .build();

        when(vehicleCcpProjectionRepository.getByBindingId(789L)).thenReturn(replaced);
        when(vehicleCcpProjectionRepository.upsertByBindingId(any(VehicleCcp.class))).thenReturn(1);

        projectionAppService.handleBindingChangedEvent(event);

        verify(vehicleCcpProjectionRepository, times(2)).upsertByBindingId(any(VehicleCcp.class));
    }

    @Test
    void handleBindingChangedEvent_NonTboxOrCcp_ShouldIgnore() {
        VehiclePartBindingChangedEvent event = VehiclePartBindingChangedEvent.builder()
            .vin("TEST_VIN")
            .bindingId(999L)
            .deviceCategory("OTHER")
            .changeType("BIND")
            .occurredAt(Instant.now())
            .seq(1L)
            .build();

        projectionAppService.handleBindingChangedEvent(event);

        verify(vehicleTboxProjectionRepository, never()).upsertByBindingId(any(VehicleTbox.class));
        verify(vehicleCcpProjectionRepository, never()).upsertByBindingId(any(VehicleCcp.class));
    }
}
