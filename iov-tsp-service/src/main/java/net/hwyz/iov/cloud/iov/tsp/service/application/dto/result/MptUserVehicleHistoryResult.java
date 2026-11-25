package net.hwyz.iov.cloud.iov.tsp.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CredentialRevocationStatus;

import java.time.Instant;

/**
 * MPT 用户车辆关系历史结果（IOV-TSP-DSN-CR-021 D021-02）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptUserVehicleHistoryResult {

    private String relationId;

    private String vin;

    private String accountId;

    private String role;

    private String state;

    private Instant boundAt;

    private Instant releasedAt;

    private String releaseReason;

    private String releaseOperationId;

    private String releasedBy;

    private CredentialRevocationStatus credentialRevocationStatus;
}
