package net.hwyz.iov.cloud.iov.tsp.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CredentialRevocationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UnbindRelationState;

import java.time.Instant;

/**
 * MPT 解绑操作查询结果（IOV-TSP-DSN-CR-021 D021-03/D021-09）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptUnbindOperationResult {

    private String operationId;

    private String relationId;

    private String vin;

    private UnbindRelationState relationState;

    private CredentialRevocationStatus credentialRevocationStatus;

    private Integer revokedSessionCount;

    private Integer retryCount;

    private Instant releasedAt;

    private Instant completedAt;

    private Instant nextRetryAt;

    private String lastError;
}
