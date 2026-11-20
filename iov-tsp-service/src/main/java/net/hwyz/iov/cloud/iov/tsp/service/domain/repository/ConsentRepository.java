package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AccountAgreementConsent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AgreementConsent;

import java.util.Optional;

/**
 * 协议同意仓储（VIN 级与 Account+VIN 级分表，禁止跨 scope 覆盖）。
 *
 * @author hwyz_leo
 */
public interface ConsentRepository {

    /**
     * 查询 VIN 级同意（幂等键：vin + 类型 + 修订 + 回执ID）
     *
     * @param vin          车辆 VIN
     * @param agreementType 协议类型
     * @param revisionId   修订标识
     * @param reportId     回执ID
     * @return 同意记录
     */
    Optional<AgreementConsent> findVinConsent(String vin, String agreementType, String revisionId, String reportId);

    /**
     * 新增 VIN 级同意
     *
     * @param consent 同意记录
     */
    void insertVinConsent(AgreementConsent consent);

    /**
     * 查询 Account+VIN 级同意
     *
     * @param accountId     账号
     * @param vin           车辆 VIN
     * @param agreementType 协议类型
     * @param revisionId    修订标识
     * @param reportId      回执ID
     * @return 同意记录
     */
    Optional<AccountAgreementConsent> findAccountConsent(String accountId, String vin, String agreementType,
                                                         String revisionId, String reportId);

    /**
     * 新增 Account+VIN 级同意
     *
     * @param consent 同意记录
     */
    void insertAccountConsent(AccountAgreementConsent consent);
}
