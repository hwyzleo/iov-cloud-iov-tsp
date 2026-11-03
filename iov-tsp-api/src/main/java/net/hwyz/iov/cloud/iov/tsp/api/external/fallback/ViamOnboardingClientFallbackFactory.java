package net.hwyz.iov.cloud.iov.tsp.api.external.fallback;

import net.hwyz.iov.cloud.iov.tsp.api.external.ViamOnboardingClient;
import net.hwyz.iov.cloud.sec.viam.api.vo.DeviceOnboardingVo;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * VIAM onboarding 快照查询 fallback（IOV-TSP-DSN-CR-016）
 * <p>
 * fail-closed：依赖不可用（超时/连接失败/熔断）抛异常，由投影链标记「未对账/陈旧」，
 * 不返回伪造状态或本地历史值兜底。
 * </p>
 *
 * @author hwyz_leo
 */
@Component
public class ViamOnboardingClientFallbackFactory implements FallbackFactory<ViamOnboardingClient> {

    @Override
    public ViamOnboardingClient create(Throwable cause) {
        return new ViamOnboardingClient() {
            @Override
            public DeviceOnboardingVo getOnboarding(String principalId) {
                throw new RuntimeException("VIAM onboarding 快照查询不可用", cause);
            }
        };
    }
}
