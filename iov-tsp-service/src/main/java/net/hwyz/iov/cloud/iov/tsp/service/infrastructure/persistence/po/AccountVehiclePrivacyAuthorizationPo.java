package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Account + VIN 隐私授权当前状态数据对象。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AccountVehiclePrivacyAuthorizationPo {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String authorizationId;

    private String accountId;

    private String vin;

    /**
     * 授权来源关系周期ID（IOV-TSP-DSN-CR-021）
     */
    private String relationId;

    private String authorizationItem;

    private String state;

    private Long version;

    private String source;

    private String reportId;

    private String payloadDigest;

    private LocalDateTime reportedAt;

    private LocalDateTime receivedAt;
}
