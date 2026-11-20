package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

import java.time.LocalDateTime;

/**
 * 绑定确认操作数据对象。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BindingOperationPo extends BasePo {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String operationId;

    private String callerScope;

    private String accountId;

    private String idempotencyKey;

    private String requestDigest;

    private String sessionId;

    private String vin;

    private String status;

    private String resultReason;

    private String resultRelationId;

    private String eligibility;

    private String ruleVersion;

    private String factRef;

    private LocalDateTime factCheckedAt;

    private LocalDateTime completedAt;
}
