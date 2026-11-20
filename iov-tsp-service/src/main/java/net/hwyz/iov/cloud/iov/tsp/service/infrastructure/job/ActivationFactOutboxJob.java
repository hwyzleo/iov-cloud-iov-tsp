package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationFact;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.ActivationFactPublisher;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationFactRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

/**
 * 首次激活候选事实 Outbox 投递任务（IOV-TSP-DSN-CR-017 D017-07）。
 * <p>
 * 提交后异步重试；Kafka 不可用不撤销已确认关系，重试不产生第二业务事实；
 * 达到最大重试次数后保留 FAILED 事实，供人工重放，不自动清空。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivationFactOutboxJob {

    private final ActivationFactRepository activationFactRepository;
    private final ActivationFactPublisher activationFactPublisher;
    private final TspUserVehicleProperties properties;
    private final Clock clock;

    /**
     * 定时排空 Outbox
     */
    @Scheduled(fixedDelayString = "${tsp.user-vehicle.activation-fact.poll-interval-ms:10000}")
    public void publishPending() {
        TspUserVehicleProperties.ActivationFact config = properties.getActivationFact();
        if (!config.isPublishEnabled()) {
            return;
        }
        List<ActivationFact> pending = activationFactRepository.findPending(config.getBatchSize());
        if (pending.isEmpty()) {
            return;
        }
        for (ActivationFact fact : pending) {
            try {
                activationFactPublisher.publish(fact);
                activationFactRepository.markPublished(fact.getEventId(), clock.instant());
            } catch (Exception e) {
                int retryCount = fact.getRetryCount() == null ? 0 : fact.getRetryCount();
                boolean exhausted = retryCount + 1 >= config.getMaxRetry();
                activationFactRepository.markFailed(fact.getEventId(), truncate(e.getMessage()),
                        exhausted ? "FAILED" : "PENDING", clock.instant());
                log.warn("投递首次激活候选事实失败: eventId={}, retryCount={}, exhausted={}",
                        fact.getEventId(), retryCount + 1, exhausted, e);
            }
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
