package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.PrivacyAuthorizationState;

import java.time.Instant;

/**
 * Account + VIN + 授权项的隐私授权当前状态。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacyAuthorization {

    private Long id;

    private String authorizationId;

    private String accountId;

    private String vin;

    /**
     * 授权来源关系周期ID（IOV-TSP-DSN-CR-021 D021-05/D021-08；历史失效周期可为空）
     */
    private String relationId;

    private String authorizationItem;

    private PrivacyAuthorizationState state;

    private Long version;

    private String source;

    private String reportId;

    private String payloadDigest;

    private Instant reportedAt;

    private Instant receivedAt;
}
