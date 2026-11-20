package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Account + VIN 级协议同意数据对象。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AccountVehicleAgreementConsentPo {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String consentId;

    private String accountId;

    private String vin;

    private String agreementType;

    private String agreementVersion;

    private String revisionId;

    private String language;

    private String contentHash;

    private String source;

    private String reportId;

    private String payloadDigest;

    private String sourceSessionId;

    private String bindingOperationId;

    private LocalDateTime reportedAgreedAt;

    private LocalDateTime receivedAt;
}
