package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.UserVehicleUnbindAppService;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 解绑后凭证撤销自动重试任务（IOV-TSP-DSN-CR-021 D021-07/D021-09）。
 * <p>
 * 只推进台账中 {@code RELEASED + PENDING/FAILED} 且到达 {@code next_retry_at} 的操作，
 * 重试沿用原 operationId（CIAM 幂等）。达到自动重试上限后保留 FAILED，仍可由具备权限的管理员人工重试。
 * 任务关闭（{@code unbind.enabled=false}）时不执行，已释放关系不自动恢复。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserVehicleRevocationRetryJob {

    private final UserVehicleUnbindAppService userVehicleUnbindAppService;
    private final TspUserVehicleProperties properties;

    /**
     * 定时重试未收敛的凭证撤销
     */
    @Scheduled(fixedDelayString = "${tsp.user-vehicle.unbind.retry-poll-interval-ms:30000}")
    public void retryPendingRevocations() {
        if (!properties.getUnbind().isEnabled()) {
            return;
        }
        userVehicleUnbindAppService.retryPendingRevocations();
    }
}
