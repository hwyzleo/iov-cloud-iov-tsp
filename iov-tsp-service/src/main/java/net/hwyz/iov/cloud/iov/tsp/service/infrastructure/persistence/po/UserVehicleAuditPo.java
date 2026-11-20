package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户车辆关系业务审计数据对象。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVehicleAuditPo {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String auditId;

    private String bizType;

    private String bizId;

    private String vin;

    private String accountId;

    private String fromState;

    private String toState;

    private String operatorSource;

    private String reason;

    private String factRef;

    /**
     * 业务迁移时刻（UTC 墙钟）
     */
    private LocalDateTime occurredAt;

    private String description;
}
