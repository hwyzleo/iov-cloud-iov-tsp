package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * VIN 级协议同意（车端跳过激活时记录，不转写为个人同意）。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementConsent {

    private Long id;

    private String consentId;

    private String vin;

    private String agreementType;

    private String version;

    private String revisionId;

    private String language;

    private String contentHash;

    private String source;

    private String reportId;

    private String payloadDigest;

    private Instant reportedAgreedAt;

    private Instant receivedAt;
}
