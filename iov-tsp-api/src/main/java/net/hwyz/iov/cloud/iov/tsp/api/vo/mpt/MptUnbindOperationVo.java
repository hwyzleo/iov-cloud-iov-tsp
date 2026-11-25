package net.hwyz.iov.cloud.iov.tsp.api.vo.mpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CredentialRevocationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UnbindRelationState;

import java.time.LocalDateTime;

/**
 * MPT 解绑操作查询结果（IOV-TSP-DSN-CR-021 D021-03/D021-09）。
 * <p>
 * 查询不到不得被解释为失败；该响应仅表达台账中已登记的权威结果。
 * 时间字段为生效展示时区的无偏移墙钟（CR-015）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptUnbindOperationVo {

    private String operationId;

    private String relationId;

    private String vin;

    private UnbindRelationState relationState;

    private CredentialRevocationStatus credentialRevocationStatus;

    private Integer revokedSessionCount;

    private Integer retryCount;

    private LocalDateTime releasedAt;

    private LocalDateTime completedAt;

    /**
     * 下次自动重试绝对时刻（无待重试时为空）
     */
    private LocalDateTime nextRetryAt;

    /**
     * 最近一次撤销失败摘要（脱敏、截断，不含 token）
     */
    private String lastError;
}
