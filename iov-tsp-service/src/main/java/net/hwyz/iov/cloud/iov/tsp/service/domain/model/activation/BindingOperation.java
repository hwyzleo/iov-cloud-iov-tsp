package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingCallerScope;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingEligibility;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingOperationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingResultReason;

import java.time.Instant;

/**
 * 绑定确认操作（幂等台账，一次确认一行）。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindingOperation {

    private Long id;

    /**
     * 操作业务ID
     */
    private String operationId;

    /**
     * 调用方范围
     */
    private BindingCallerScope callerScope;

    /**
     * 账号标识
     */
    private String accountId;

    /**
     * 幂等键
     */
    private String idempotencyKey;

    /**
     * 请求内容不可逆摘要
     */
    private String requestDigest;

    /**
     * 来源会话ID
     */
    private String sessionId;

    /**
     * 车辆 VIN
     */
    private String vin;

    /**
     * 操作状态
     */
    private BindingOperationStatus status;

    /**
     * 失败/冲突语义 reason
     */
    private BindingResultReason resultReason;

    /**
     * 成功时关联的关系ID
     */
    private String resultRelationId;

    /**
     * 资格判定
     */
    private BindingEligibility eligibility;

    /**
     * 资格规则版本
     */
    private String ruleVersion;

    /**
     * 事实引用摘要
     */
    private String factRef;

    /**
     * 事实读取时刻
     */
    private Instant factCheckedAt;

    /**
     * 终态时刻
     */
    private Instant completedAt;

    /**
     * 乐观版本
     */
    private Integer rowVersion;
}
