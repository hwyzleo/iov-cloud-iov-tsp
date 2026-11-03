package net.hwyz.iov.cloud.iov.tsp.api.external;

import net.hwyz.iov.cloud.iov.tsp.api.external.fallback.ViamOnboardingClientFallbackFactory;
import net.hwyz.iov.cloud.sec.viam.api.vo.DeviceOnboardingVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * VIAM 官方安全开通快照查询客户端（IOV-TSP-DSN-CR-016）
 * <p>
 * 对应 VIAM 官方端点 {@code GET /api/service/deviceLogin/v1/onboarding/{principalId}}
 * （服务名 {@code sec-viam}）。TSP 仅消费 VIAM 权威快照做只读投影，不自行推断安全状态。
 * 当前 VIAM 快照未携带 securityVersion（V-GAP-1），版本列预留；activate_time 语义未明确
 * （V-GAP-3），快照应用不写 activate_time。
 * </p>
 *
 * @author hwyz_leo
 */
@FeignClient(name = "sec-viam", path = "/api/service/deviceLogin/v1",
        fallbackFactory = ViamOnboardingClientFallbackFactory.class)
public interface ViamOnboardingClient {

    /**
     * 查询设备安全开通快照（VIAM 权威）。
     *
     * @param principalId VIAM 安全主体ID
     * @return 官方快照 VO；依赖不可用时由 fallback 抛异常（fail-closed）
     */
    @GetMapping("/onboarding/{principalId}")
    DeviceOnboardingVo getOnboarding(@PathVariable("principalId") String principalId);
}
