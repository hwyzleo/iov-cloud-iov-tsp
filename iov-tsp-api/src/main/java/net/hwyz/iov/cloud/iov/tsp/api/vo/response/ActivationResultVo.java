package net.hwyz.iov.cloud.iov.tsp.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ActivationSessionState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.IviLoginResultStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.RelationSource;

import java.time.Instant;

/**
 * 车端绑定结果与恢复窗口。
 * <p>
 * 只返回本车绑定结果；不包含通用账号令牌或车机登录材料。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationResultVo {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 会话状态
     */
    private ActivationSessionState state;

    /**
     * 绑定操作ID
     */
    private String bindingOperationId;

    /**
     * 关系ID（已绑定时；不得当作跨时间稳定主键，来源见 relationSource）
     */
    private String relationId;

    /**
     * 关系ID来源（IOV-TSP-DSN-CR-020 D020-08 / R-6 纯新增字段）：
     * BINDING_OPERATION 为本次绑定操作 resultRelationId；ACTIVE_RELATION 为回退的当前有效关系
     */
    private RelationSource relationSource;

    /**
     * 车辆 VIN
     */
    private String vin;

    /**
     * 车机登录结果（独立于绑定状态）
     */
    private IviLoginResultStatus iviLoginState;

    /**
     * 结果恢复窗口截止绝对时刻
     */
    private Instant resultExpiresAt;
}
