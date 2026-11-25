package net.hwyz.iov.cloud.iov.tsp.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CredentialRevocationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UnbindRelationState;

import java.time.Instant;

/**
 * MPT 人工解绑/重试结果（IOV-TSP-DSN-CR-021 D021-03）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptUserVehicleUnbindResult {

    private String operationId;

    private String relationId;

    private String vin;

    private UnbindRelationState relationState;

    private CredentialRevocationStatus credentialRevocationStatus;

    private Instant releasedAt;

    private Integer revokedSessionCount;
}
