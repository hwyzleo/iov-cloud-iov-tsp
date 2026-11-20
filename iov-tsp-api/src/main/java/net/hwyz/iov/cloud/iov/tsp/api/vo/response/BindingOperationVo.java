package net.hwyz.iov.cloud.iov.tsp.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingOperationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingResultReason;

import java.time.Instant;

/**
 * 绑定操作结果查询（仅本人操作；查询不到不等于已失败）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindingOperationVo {

    /**
     * 绑定操作ID
     */
    private String operationId;

    /**
     * 操作状态
     */
    private BindingOperationStatus status;

    /**
     * 失败/冲突语义 reason
     */
    private BindingResultReason reason;

    /**
     * 成功时的关系ID
     */
    private String relationId;

    /**
     * 车辆 VIN
     */
    private String vin;

    /**
     * 来源会话ID
     */
    private String sessionId;

    /**
     * 完成绝对时刻（PENDING 时为空）
     */
    private Instant completedAt;
}
