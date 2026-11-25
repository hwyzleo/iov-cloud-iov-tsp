package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

import java.time.LocalDateTime;

/**
 * 用户车辆关系人工解绑操作与 CIAM 凭证撤销台账数据对象（IOV-TSP-DSN-CR-021）。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserVehicleUnbindOperationPo extends BasePo {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String operationId;

    private String relationId;

    private String accountId;

    private String vin;

    private String operatorId;

    private String idempotencyKey;

    private String requestDigest;

    private Integer expectedRowVersion;

    private String reason;

    private String relationState;

    private LocalDateTime releasedAt;

    private LocalDateTime completedAt;

    private String credentialRevocationStatus;

    private Integer revokedSessionCount;

    private Integer retryCount;

    private LocalDateTime nextRetryAt;

    private String lastError;
}
