package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * VIN 级协议同意数据对象。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleAgreementConsentPo {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String consentId;

    private String vin;

    private String agreementType;

    private String agreementVersion;

    private String revisionId;

    private String language;

    private String contentHash;

    private String source;

    private String reportId;

    private String payloadDigest;

    private LocalDateTime reportedAgreedAt;

    private LocalDateTime receivedAt;
}
