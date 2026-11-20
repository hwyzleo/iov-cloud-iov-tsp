package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.UtcTime;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AccountAgreementConsent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AgreementConsent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ConsentRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.ConsentMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.AccountVehicleAgreementConsentPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleAgreementConsentPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 协议同意仓储实现（VIN 级与 Account+VIN 级分表，禁止跨 scope 覆盖）。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class ConsentRepositoryImpl implements ConsentRepository {

    private final ConsentMapper consentMapper;

    @Override
    public Optional<AgreementConsent> findVinConsent(String vin, String agreementType, String revisionId,
                                                     String reportId) {
        return Optional.ofNullable(consentMapper.selectVinConsent(vin, agreementType, revisionId, reportId))
                .map(po -> AgreementConsent.builder()
                        .id(po.getId())
                        .consentId(po.getConsentId())
                        .vin(po.getVin())
                        .agreementType(po.getAgreementType())
                        .version(po.getAgreementVersion())
                        .revisionId(po.getRevisionId())
                        .language(po.getLanguage())
                        .contentHash(po.getContentHash())
                        .source(po.getSource())
                        .reportId(po.getReportId())
                        .payloadDigest(po.getPayloadDigest())
                        .reportedAgreedAt(UtcTime.toInstant(po.getReportedAgreedAt()))
                        .receivedAt(UtcTime.toInstant(po.getReceivedAt()))
                        .build());
    }

    @Override
    public void insertVinConsent(AgreementConsent consent) {
        VehicleAgreementConsentPo po = VehicleAgreementConsentPo.builder()
                .consentId(consent.getConsentId())
                .vin(consent.getVin())
                .agreementType(consent.getAgreementType())
                .agreementVersion(consent.getVersion())
                .revisionId(consent.getRevisionId())
                .language(consent.getLanguage())
                .contentHash(consent.getContentHash())
                .source(consent.getSource())
                .reportId(consent.getReportId())
                .payloadDigest(consent.getPayloadDigest())
                .reportedAgreedAt(UtcTime.toLocalDateTime(consent.getReportedAgreedAt()))
                .receivedAt(UtcTime.toLocalDateTime(consent.getReceivedAt()))
                .build();
        consentMapper.insertVinConsent(po);
        consent.setId(po.getId());
    }

    @Override
    public Optional<AccountAgreementConsent> findAccountConsent(String accountId, String vin, String agreementType,
                                                               String revisionId, String reportId) {
        return Optional.ofNullable(consentMapper.selectAccountConsent(accountId, vin, agreementType, revisionId, reportId))
                .map(po -> AccountAgreementConsent.builder()
                        .id(po.getId())
                        .consentId(po.getConsentId())
                        .accountId(po.getAccountId())
                        .vin(po.getVin())
                        .agreementType(po.getAgreementType())
                        .version(po.getAgreementVersion())
                        .revisionId(po.getRevisionId())
                        .language(po.getLanguage())
                        .contentHash(po.getContentHash())
                        .source(po.getSource())
                        .reportId(po.getReportId())
                        .payloadDigest(po.getPayloadDigest())
                        .sourceSessionId(po.getSourceSessionId())
                        .bindingOperationId(po.getBindingOperationId())
                        .reportedAgreedAt(UtcTime.toInstant(po.getReportedAgreedAt()))
                        .receivedAt(UtcTime.toInstant(po.getReceivedAt()))
                        .build());
    }

    @Override
    public void insertAccountConsent(AccountAgreementConsent consent) {
        AccountVehicleAgreementConsentPo po = AccountVehicleAgreementConsentPo.builder()
                .consentId(consent.getConsentId())
                .accountId(consent.getAccountId())
                .vin(consent.getVin())
                .agreementType(consent.getAgreementType())
                .agreementVersion(consent.getVersion())
                .revisionId(consent.getRevisionId())
                .language(consent.getLanguage())
                .contentHash(consent.getContentHash())
                .source(consent.getSource())
                .reportId(consent.getReportId())
                .payloadDigest(consent.getPayloadDigest())
                .sourceSessionId(consent.getSourceSessionId())
                .bindingOperationId(consent.getBindingOperationId())
                .reportedAgreedAt(UtcTime.toLocalDateTime(consent.getReportedAgreedAt()))
                .receivedAt(UtcTime.toLocalDateTime(consent.getReceivedAt()))
                .build();
        consentMapper.insertAccountConsent(po);
        consent.setId(po.getId());
    }
}
