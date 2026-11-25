package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CredentialRevocationStatus;

import java.time.Instant;

/**
 * MPT 用户车辆关系历史行（IOV-TSP-DSN-CR-021 D021-02）。
 * <p>
 * 时间保持 UTC 绝对事实；展示墙钟在 Adapter/VO 边界投影。
 *
 * @param relationId                 关系ID
 * @param vin                        车辆 VIN
 * @param accountId                  账号（Adapter 负责脱敏）
 * @param role                       关系角色
 * @param state                      ACTIVE/RELEASED
 * @param boundAt                    绑定绝对时刻
 * @param releasedAt                 释放绝对时刻
 * @param releaseReason              解绑原因
 * @param releaseOperationId         解绑操作ID
 * @param releasedBy                 可信操作人
 * @param credentialRevocationStatus 关联解绑操作的撤销状态（可空）
 * @author hwyz_leo
 */
public record MptUserVehicleHistoryItem(String relationId, String vin, String accountId, String role, String state,
                                        Instant boundAt, Instant releasedAt, String releaseReason,
                                        String releaseOperationId, String releasedBy,
                                        CredentialRevocationStatus credentialRevocationStatus) {
}
