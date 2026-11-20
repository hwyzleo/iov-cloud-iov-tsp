package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Account + VIN 级协议同意（可信车机登录成功回执后记录）。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountAgreementConsent {

    private Long id;

    private String consentId;

    private String accountId;

    private String vin;

    private String agreementType;

    private String version;

    private String revisionId;

    private String language;

    private String contentHash;

    private String source;

    private String reportId;

    private String payloadDigest;

    private String sourceSessionId;

    private String bindingOperationId;

    private Instant reportedAgreedAt;

    private Instant receivedAt;
}
