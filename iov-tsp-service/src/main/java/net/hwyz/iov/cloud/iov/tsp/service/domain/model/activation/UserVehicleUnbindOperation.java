package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CredentialRevocationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UnbindRelationState;

import java.time.Instant;

/**
 * 用户车辆关系人工解绑操作与 CIAM 凭证撤销台账（IOV-TSP-DSN-CR-021 D021-04/D021-05）。
 * <p>
 * 本地关系释放与 CIAM 撤销是两个独立事实：本台账先记录关系释放结果，
 * 再以 {@code credentialRevocationStatus} 独立表达撤销进度与重试。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVehicleUnbindOperation {

    private Long id;

    /**
     * 解绑操作业务ID（对外引用与 CIAM 幂等键）
     */
    private String operationId;

    private String relationId;

    private String accountId;

    private String vin;

    /**
     * 可信 MPT 操作人
     */
    private String operatorId;

    /**
     * 操作人提供的幂等键
     */
    private String idempotencyKey;

    /**
     * 请求内容不可逆摘要（同键异体即冲突）
     */
    private String requestDigest;

    /**
     * 请求声明的预期关系版本
     */
    private Integer expectedRowVersion;

    /**
     * 解绑原因码
     */
    private String reason;

    /**
     * 关系状态（本地事务提交后固定 RELEASED）
     */
    private UnbindRelationState relationState;

    private Instant releasedAt;

    private Instant completedAt;

    /**
     * CIAM 凭证撤销状态（独立于关系状态）
     */
    private CredentialRevocationStatus credentialRevocationStatus;

    /**
     * CIAM 本次实际撤销会话数
     */
    private Integer revokedSessionCount;

    /**
     * 撤销重试次数
     */
    private Integer retryCount;

    /**
     * 下次自动重试绝对时刻
     */
    private Instant nextRetryAt;

    /**
     * 最近一次撤销失败摘要（脱敏、截断）
     */
    private String lastError;

    private Integer rowVersion;
}
