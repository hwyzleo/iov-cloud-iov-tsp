package net.hwyz.iov.cloud.iov.tsp.api.vo.mpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CredentialRevocationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UnbindRelationState;

import java.time.Instant;

/**
 * MPT 人工解绑/人工重试响应（IOV-TSP-DSN-CR-021 D021-03）。
 * <p>
 * 成功码只表示请求已权威受理且关系状态明确；{@code relationState=RELEASED} 与
 * {@code credentialRevocationStatus=PENDING/FAILED} 可同时出现，表示关系已释放但凭证撤销未完成。
 * 时间字段为绝对 UTC 时刻（带 Z），与 D021-03 示例一致。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptUserVehicleUnbindVo {

    private String operationId;

    private String relationId;

    private String vin;

    private UnbindRelationState relationState;

    private CredentialRevocationStatus credentialRevocationStatus;

    /**
     * 关系释放绝对时刻（UTC）
     */
    private Instant releasedAt;

    /**
     * 本次实际撤销会话数（撤销未完成时为空）
     */
    private Integer revokedSessionCount;
}
