package net.hwyz.iov.cloud.iov.tsp.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingOperationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingResultReason;

import java.time.Instant;

/**
 * 确认绑定结果（幂等：同键同内容返回原结果）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmVehicleBindingVo {

    /**
     * 绑定操作ID（结果恢复引用）
     */
    private String operationId;

    /**
     * 操作状态
     */
    private BindingOperationStatus status;

    /**
     * 失败/冲突语义 reason（成功时为空）
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
     * 完成绝对时刻
     */
    private Instant completedAt;
}
