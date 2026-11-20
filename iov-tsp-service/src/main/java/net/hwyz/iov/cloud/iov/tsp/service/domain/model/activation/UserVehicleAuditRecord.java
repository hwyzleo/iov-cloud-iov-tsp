package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 用户车辆关系业务审计记录（一次业务迁移一行，不含明文二维码/令牌）。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVehicleAuditRecord {

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

    private Instant occurredAt;
}
