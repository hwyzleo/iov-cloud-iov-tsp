package net.hwyz.iov.cloud.iov.tsp.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UserVehicleRelationStatus;

import java.time.Instant;

/**
 * 当前账号在指定 VIN 上的人车关系应用查询结果（IOV-TSP-DSN-CR-018 D018-02）。
 * <p>
 * 应用层只表达服务端二值事实，不依赖 API VO；由 Adapter 显式转换为响应对象。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVehicleRelationResult {

    /**
     * 关系状态：BOUND/NOT_BOUND
     */
    private UserVehicleRelationStatus status;

    /**
     * 请求 VIN（已规范化）
     */
    private String vin;

    /**
     * 当前账号有效关系ID（NOT_BOUND 为空）
     */
    private String relationId;

    /**
     * 当前关系角色（NOT_BOUND 为空）
     */
    private String role;

    /**
     * 绑定绝对时刻（NOT_BOUND 为空）
     */
    private Instant boundAt;
}
