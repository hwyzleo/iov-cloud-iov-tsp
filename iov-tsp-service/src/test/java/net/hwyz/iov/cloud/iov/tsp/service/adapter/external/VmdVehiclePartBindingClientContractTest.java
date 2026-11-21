package net.hwyz.iov.cloud.iov.tsp.service.adapter.external;

import net.hwyz.iov.cloud.iov.tsp.api.external.VmdVehiclePartBindingClient;
import net.hwyz.iov.cloud.iov.tsp.api.external.fallback.VmdVehiclePartBindingClientFallbackFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VMD 绑定查询客户端契约一致性测试（IOV-TSP-DSN-CR-017 校正）。
 * <p>
 * 历史客户端路径/方法与 EDD-VMD 实际发布契约不一致（调用必然 404），本测试锁定正确契约，
 * 防止后续回退成 {@code /api/service/vmd/v1/vehiclePartBinding/listActiveByVin}。
 */
class VmdVehiclePartBindingClientContractTest {

    @Test
    @DisplayName("服务名与基础路径与 VMD 实际契约一致：edd-vmd + /api/service/vehiclePartBinding/v1")
    void basePathMatchesVmdContract() {
        FeignClient feignClient = VmdVehiclePartBindingClient.class.getAnnotation(FeignClient.class);
        assertThat(feignClient).isNotNull();
        assertThat(feignClient.path()).isEqualTo("/api/service/vehiclePartBinding/v1");
        assertThat(feignClient.name()).isEqualTo("edd-vmd");
        assertThat(feignClient.fallbackFactory()).isEqualTo(VmdVehiclePartBindingClientFallbackFactory.class);
    }

    @Test
    @DisplayName("按 VIN 查 active 绑定使用路径参数 /vin/{vin}/active")
    void listActiveBindingsByVinUsesPathParam() throws Exception {
        Method method = VmdVehiclePartBindingClient.class.getMethod("listActiveBindingsByVin", String.class);
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/vin/{vin}/active");
        assertThat(method.getParameters()[0].getAnnotation(PathVariable.class)).isNotNull();
        assertThat(method.getParameters()[0].getAnnotation(PathVariable.class).value()).isEqualTo("vin");
    }

    @Test
    @DisplayName("bootstrap 使用 /bootstrap 且 cursor 可选")
    void bootstrapMatchesVmdContract() throws Exception {
        Method method = VmdVehiclePartBindingClient.class
                .getMethod("listBindingsForBootstrap", Long.class, Integer.class);
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/bootstrap");
    }
}
