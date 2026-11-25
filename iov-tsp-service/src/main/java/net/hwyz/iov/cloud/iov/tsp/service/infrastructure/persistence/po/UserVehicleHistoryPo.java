package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MPT 用户车辆关系历史分页行数据对象（IOV-TSP-DSN-CR-021）。
 * <p>
 * 由专用查询 SQL 一次性取关系与解绑撤销状态，避免历史列表逐行查询。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVehicleHistoryPo {

    private String relationId;

    private String vin;

    private String accountId;

    private String role;

    private String state;

    private LocalDateTime boundAt;

    private LocalDateTime releasedAt;

    private String releaseReason;

    private String releaseOperationId;

    private String releasedBy;

    private String credentialRevocationStatus;
}
