package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.support;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.UserVehicleBindingErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 可信调用方上下文与 CIAM 设备令牌契约对齐测试（IOV-TSP-DSN-CR-017 / SEC-CIAM-DSN-CR-005）。
 * <p>
 * CIAM 设备令牌 {@code sub=device:<principal>}，SGW 会把它注入 {@code X-User-Id}；
 * TSP 必须拒绝把设备主体当账号，避免污染 Account+VIN 的隐私授权与同意记录。
 */
class TrustedCallerResolverTest {

    private final TrustedCallerResolver resolver = new TrustedCallerResolver(new TspUserVehicleProperties());

    @AfterEach
    void tearDown() {
        SecurityContextHolder.remove();
    }

    @Test
    @DisplayName("设备令牌主体（device: 前缀）不得作为账号使用")
    void deviceSubjectRejectedAsAccount() {
        SecurityContextHolder.setUserId("device:IVI-PRINCIPAL-1");
        assertThatThrownBy(resolver::requireAccountId)
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.INVALID_REQUEST.getCode()));
    }

    @Test
    @DisplayName("用户令牌主体可作为账号（非 device: 前缀）")
    void userSubjectAcceptedAsAccount() {
        SecurityContextHolder.setUserId("account-1");
        assertThat(resolver.requireAccountId()).isEqualTo("account-1");
    }

    @Test
    @DisplayName("车端入口要求 client_type=IVI 且必须注入 X-Vin")
    void vehicleRequiresIviAndVin() {
        SecurityContextHolder.setClientType("IVI");
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThatThrownBy(() -> resolver.requireVehicleVin(request))
                .isInstanceOf(BusinessException.class);
        request.addHeader("X-Vin", "lsvaa1234567890ab");
        assertThat(resolver.requireVehicleVin(request)).isEqualTo("LSVAA1234567890AB");
    }

    @Test
    @DisplayName("App 端入口要求 client_type=MOBILE，车机令牌不得调用")
    void appRequiresMobileClientType() {
        SecurityContextHolder.setClientType("IVI");
        SecurityContextHolder.setUserId("account-1");
        assertThatThrownBy(resolver::requireAppAccount).isInstanceOf(BusinessException.class);
        SecurityContextHolder.setClientType("MOBILE");
        assertThat(resolver.requireAppAccount()).isEqualTo("account-1");
    }
}
