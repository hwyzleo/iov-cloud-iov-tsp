package net.hwyz.iov.cloud.iov.tsp.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UserVehicleRelationStatus;

import java.time.Instant;

/**
 * 当前账号在指定 VIN 上的人车关系查询结果（IOV-TSP-DSN-CR-018 D018-01）。
 * <p>
 * 仅暴露当前账号自身关系的最小字段；NOT_BOUND 时除 status/vin 外均为 null，
 * 不输出 accountId、state、factRef、sourceOperationId、ruleVersion、rowVersion 或数据库主键。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVehicleRelationVo {

    /**
     * 关系状态：BOUND/NOT_BOUND
     */
    private UserVehicleRelationStatus status;

    /**
     * 请求 VIN（规范化后）
     */
    private String vin;

    /**
     * 当前账号有效关系ID（NOT_BOUND 为空）
     */
    private String relationId;

    /**
     * 当前关系角色，如 PRIMARY_USER（NOT_BOUND 为空）
     */
    private String role;

    /**
     * 当前关系绑定绝对时刻（NOT_BOUND 为空）
     */
    private Instant boundAt;
}
