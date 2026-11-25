package net.hwyz.iov.cloud.iov.tsp.api.vo.mpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CredentialRevocationStatus;

import java.time.LocalDateTime;

/**
 * MPT 用户车辆关系历史分页项（IOV-TSP-DSN-CR-021 D021-02）。
 * <p>
 * 历史关系与解绑操作均保留可追溯；账号脱敏，时间按生效展示时区输出墙钟。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptUserVehicleHistoryVo {

    private String relationId;

    private String vin;

    private String maskedAccountId;

    private String role;

    /**
     * ACTIVE/RELEASED
     */
    private String state;

    private LocalDateTime boundAt;

    private LocalDateTime releasedAt;

    private String releaseReason;

    private String releaseOperationId;

    private String releasedBy;

    /**
     * 关联解绑操作的撤销状态（无解绑操作时为空）
     */
    private CredentialRevocationStatus credentialRevocationStatus;
}
