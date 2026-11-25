package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import net.hwyz.iov.cloud.iov.tsp.api.external.fallback.CiamBindingRevocationClientFallbackFactory;
import net.hwyz.iov.cloud.iov.tsp.api.external.fallback.VmdVehicleActivationClientFallbackFactory;
import net.hwyz.iov.cloud.iov.tsp.api.external.fallback.VmdVehiclePartBindingClientFallbackFactory;
import net.hwyz.iov.cloud.mkt.cms.api.fallback.AgreementContentServiceFallbackFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 用户车辆关系模块外部契约降级装配（IOV-TSP-DSN-CR-017）。
 * <p>
 * 外部契约的 FallbackFactory 位于各自 api 制品包内，不在本服务组件扫描路径下；
 * 这里显式注册，保证 Feign 客户端按「依赖不可用 → fail-closed」语义装配，
 * 不使用空内容或旧缓存伪装依赖成功。
 *
 * @author hwyz_leo
 */
@Configuration
public class TspUserVehicleFeignConfig {

    /**
     * VMD 车辆事实降级工厂（抛依赖不可用，不放行）
     *
     * @return 降级工厂
     */
    @Bean
    public VmdVehiclePartBindingClientFallbackFactory vmdVehiclePartBindingClientFallbackFactory() {
        return new VmdVehiclePartBindingClientFallbackFactory();
    }

    /**
     * CMS 协议内容降级工厂（内容不可用即拒绝，不伪造协议）
     *
     * @return 降级工厂
     */
    @Bean
    public AgreementContentServiceFallbackFactory agreementContentServiceFallbackFactory() {
        return new AgreementContentServiceFallbackFactory();
    }

    /**
     * VMD 首次激活事实降级工厂（查询不可用即 UNKNOWN/PARTIAL，不伪造 NOT_RECORDED）
     *
     * @return 降级工厂
     */
    @Bean
    public VmdVehicleActivationClientFallbackFactory vmdVehicleActivationClientFallbackFactory() {
        return new VmdVehicleActivationClientFallbackFactory();
    }

    /**
     * CIAM 绑定撤销降级工厂（不可用即台账 PENDING/FAILED，不伪造 SUCCEEDED）
     *
     * @return 降级工厂
     */
    @Bean
    public CiamBindingRevocationClientFallbackFactory ciamBindingRevocationClientFallbackFactory() {
        return new CiamBindingRevocationClientFallbackFactory();
    }
}
