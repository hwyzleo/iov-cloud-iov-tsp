package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 用户绑车首次激活候选事实 + 投递 Outbox（IOV-TSP-DSN-CR-017 D017-07）。
 * <p>
 * 与绑定回执状态同事务写入；提交后异步重试投递。Kafka 不可用不回滚已确认关系，
 * 重试不产生第二业务事实（VMD 按 eventId 幂等）。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationFact {

    private Long id;

    /**
     * 候选事实事件ID（幂等键）
     */
    private String eventId;

    /**
     * 事件契约版本
     */
    private Integer schemaVersion;

    /**
     * 车辆 VIN
     */
    private String vin;

    /**
     * 绑定操作ID
     */
    private String bindingOperationId;

    /**
     * 来源事实引用
     */
    private String sourceFactRef;

    /**
     * 事实发生绝对时刻
     */
    private Instant occurredAt;

    /**
     * 载荷摘要
     */
    private String payloadDigest;

    /**
     * 投递状态 PENDING/PUBLISHED/FAILED
     */
    private String publishStatus;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最近错误摘要
     */
    private String lastError;

    /**
     * 投递成功时刻
     */
    private Instant publishedAt;
}
