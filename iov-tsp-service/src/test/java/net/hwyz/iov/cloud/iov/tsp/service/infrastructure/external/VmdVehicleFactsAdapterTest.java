package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.external;

import net.hwyz.iov.cloud.iov.tsp.api.external.VmdVehiclePartBindingClient;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.VehicleFacts;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.DependencyUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * VMD 车辆事实防腐层测试（IOV-TSP-DSN-CR-017 / EDD-VMD-DSN-CR-076 边界）。
 * <p>
 * TSP 只按 VIN 读取 active 绑定（device→VIN 查询归 CIAM 使用）；无绑定按不可用处理，依赖异常不得当作无绑定。
 */
@ExtendWith(MockitoExtension.class)
class VmdVehicleFactsAdapterTest {

    @Mock
    private VmdVehiclePartBindingClient vmdVehiclePartBindingClient;

    @InjectMocks
    private VmdVehicleFactsAdapter adapter;

    @Test
    @DisplayName("存在 active 绑定时视为车辆可用")
    void presentWhenActiveBinding() {
        when(vmdVehiclePartBindingClient.listActiveBindingsByVin("LSVAA1234567890AB")).thenReturn(List.of(
                new VmdVehiclePartBindingClient.VehiclePartBindingDto(1L, "LSVAA1234567890AB",
                        "PART-1", "SN-1", "TBOX", "NODE-1")));
        assertThat(adapter.load("LSVAA1234567890AB").present()).isTrue();
    }

    @Test
    @DisplayName("无 active 绑定按不可用处理")
    void missingWhenNoBinding() {
        when(vmdVehiclePartBindingClient.listActiveBindingsByVin("LSVAA1234567890AB")).thenReturn(List.of());
        VehicleFacts facts = adapter.load("LSVAA1234567890AB");
        assertThat(facts.present()).isFalse();
        assertThat(facts.vin()).isEqualTo("LSVAA1234567890AB");
    }

    @Test
    @DisplayName("依赖异常按不可用抛出，不当作无绑定")
    void dependencyFailure() {
        when(vmdVehiclePartBindingClient.listActiveBindingsByVin("LSVAA1234567890AB"))
                .thenThrow(new RuntimeException("vmd down"));
        assertThatThrownBy(() -> adapter.load("LSVAA1234567890AB"))
                .isInstanceOf(DependencyUnavailableException.class);
    }
}
