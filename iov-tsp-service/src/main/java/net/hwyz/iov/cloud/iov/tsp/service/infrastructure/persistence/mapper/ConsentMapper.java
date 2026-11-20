package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.AccountVehicleAgreementConsentPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleAgreementConsentPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 协议同意 DAO（VIN 级与 Account+VIN 级分表）。
 *
 * @author hwyz_leo
 */
@Mapper
public interface ConsentMapper {

    String VIN_COLUMNS = "id, consent_id AS consentId, vin, agreement_type AS agreementType, "
            + "agreement_version AS agreementVersion, revision_id AS revisionId, language, content_hash AS contentHash, "
            + "source, report_id AS reportId, payload_digest AS payloadDigest, "
            + "reported_agreed_at AS reportedAgreedAt, received_at AS receivedAt";

    String ACCOUNT_COLUMNS = "id, consent_id AS consentId, account_id AS accountId, vin, "
            + "agreement_type AS agreementType, agreement_version AS agreementVersion, revision_id AS revisionId, "
            + "language, content_hash AS contentHash, source, report_id AS reportId, payload_digest AS payloadDigest, "
            + "source_session_id AS sourceSessionId, binding_operation_id AS bindingOperationId, "
            + "reported_agreed_at AS reportedAgreedAt, received_at AS receivedAt";

    /**
     * 查询 VIN 级同意（幂等键）
     */
    @Select("SELECT " + VIN_COLUMNS + " FROM tb_vehicle_agreement_consent "
            + "WHERE vin = #{vin} AND agreement_type = #{agreementType} AND revision_id = #{revisionId} "
            + "AND report_id = #{reportId} AND row_valid = 1 LIMIT 1")
    VehicleAgreementConsentPo selectVinConsent(@Param("vin") String vin,
                                               @Param("agreementType") String agreementType,
                                               @Param("revisionId") String revisionId,
                                               @Param("reportId") String reportId);

    /**
     * 新增 VIN 级同意
     */
    @Insert("INSERT INTO tb_vehicle_agreement_consent (consent_id, vin, agreement_type, agreement_version, "
            + "revision_id, language, content_hash, source, report_id, payload_digest, reported_agreed_at, "
            + "received_at, row_version, row_valid) "
            + "VALUES (#{consentId}, #{vin}, #{agreementType}, #{agreementVersion}, #{revisionId}, #{language}, "
            + "#{contentHash}, #{source}, #{reportId}, #{payloadDigest}, #{reportedAgreedAt}, #{receivedAt}, 1, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertVinConsent(VehicleAgreementConsentPo po);

    /**
     * 查询 Account+VIN 级同意（幂等键）
     */
    @Select("SELECT " + ACCOUNT_COLUMNS + " FROM tb_account_vehicle_agreement_consent "
            + "WHERE account_id = #{accountId} AND vin = #{vin} AND agreement_type = #{agreementType} "
            + "AND revision_id = #{revisionId} AND report_id = #{reportId} AND row_valid = 1 LIMIT 1")
    AccountVehicleAgreementConsentPo selectAccountConsent(@Param("accountId") String accountId,
                                                          @Param("vin") String vin,
                                                          @Param("agreementType") String agreementType,
                                                          @Param("revisionId") String revisionId,
                                                          @Param("reportId") String reportId);

    /**
     * 新增 Account+VIN 级同意
     */
    @Insert("INSERT INTO tb_account_vehicle_agreement_consent (consent_id, account_id, vin, agreement_type, "
            + "agreement_version, revision_id, language, content_hash, source, report_id, payload_digest, "
            + "source_session_id, binding_operation_id, reported_agreed_at, received_at, row_version, row_valid) "
            + "VALUES (#{consentId}, #{accountId}, #{vin}, #{agreementType}, #{agreementVersion}, #{revisionId}, "
            + "#{language}, #{contentHash}, #{source}, #{reportId}, #{payloadDigest}, #{sourceSessionId}, "
            + "#{bindingOperationId}, #{reportedAgreedAt}, #{receivedAt}, 1, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAccountConsent(AccountVehicleAgreementConsentPo po);
}
