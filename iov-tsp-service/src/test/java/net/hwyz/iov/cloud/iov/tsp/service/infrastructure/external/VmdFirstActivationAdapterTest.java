package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.external;

import net.hwyz.iov.cloud.iov.tsp.api.external.VmdVehicleActivationClient;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.FirstActivationState;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.FirstActivationFact;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.DependencyUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * VMD 首次激活事实只读适配测试（IOV-TSP-DSN-CR-021 TC021-03）。
 */
@ExtendWith(MockitoExtension.class)
class VmdFirstActivationAdapterTest {

    private static final String VIN = "LSVAA1234567890AB";

    @Mock
    private VmdVehicleActivationClient client;

    @Test
    @DisplayName("RECORDED：原样映射三态与事实时刻")
    void recordedMapped() {
        when(client.getFirstActivation(VIN)).thenReturn(new VmdVehicleActivationClient.FirstActivationDto(
                "RECORDED", VIN, Instant.parse("2026-11-25T02:00:00Z"), "vehicleActivation:ref"));

        FirstActivationFact fact = new VmdFirstActivationAdapter(client).load(VIN);

        assertThat(fact.state()).isEqualTo(FirstActivationState.RECORDED);
        assertThat(fact.firstActivatedAt()).isEqualTo(Instant.parse("2026-11-25T02:00:00Z"));
    }

    @Test
    @DisplayName("NOT_RECORDED：保持无事实，不伪造 UNKNOWN")
    void notRecordedMapped() {
        when(client.getFirstActivation(VIN)).thenReturn(
                new VmdVehicleActivationClient.FirstActivationDto("NOT_RECORDED", VIN, null, null));

        assertThat(new VmdFirstActivationAdapter(client).load(VIN).state())
                .isEqualTo(FirstActivationState.NOT_RECORDED);
    }

    @Test
    @DisplayName("技术失败/非法响应：依赖不可用，不降级为 NOT_RECORDED")
    void failureIsDependencyUnavailable() {
        when(client.getFirstActivation(VIN)).thenThrow(new IllegalStateException("timeout"));
        assertThatThrownBy(() -> new VmdFirstActivationAdapter(client).load(VIN))
                .isInstanceOf(DependencyUnavailableException.class);

        org.mockito.Mockito.doReturn(new VmdVehicleActivationClient.FirstActivationDto("UNKNOWN", VIN, null, null))
                .when(client).getFirstActivation(VIN);
        assertThatThrownBy(() -> new VmdFirstActivationAdapter(client).load(VIN))
                .isInstanceOf(DependencyUnavailableException.class);
    }
}
