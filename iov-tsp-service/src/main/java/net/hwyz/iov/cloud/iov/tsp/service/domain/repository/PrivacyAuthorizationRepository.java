package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.PrivacyAuthorization;

import java.util.Optional;

/**
 * Account + VIN + 关系周期隐私授权仓储（IOV-TSP-DSN-CR-021 D021-05/D021-08）。
 * <p>
 * 当前记录以 {@code (accountId, vin, relationId, authorizationItem)} 唯一；
 * 历史失效周期保留原行，不物理删除。
 *
 * @author hwyz_leo
 */
public interface PrivacyAuthorizationRepository {

    /**
     * 查询同一关系周期内授权项当前状态
     *
     * @param accountId         账号
     * @param vin               车辆 VIN
     * @param relationId        关系周期ID
     * @param authorizationItem 授权项
     * @return 授权记录
     */
    Optional<PrivacyAuthorization> find(String accountId, String vin, String relationId, String authorizationItem);

    /**
     * 新增授权记录
     *
     * @param authorization 授权记录
     */
    void insert(PrivacyAuthorization authorization);

    /**
     * 仅当来源版本更新时覆盖（乱序/旧版本不得覆盖新状态）
     *
     * @param authorization 授权记录
     * @return 更新成功返回 true
     */
    boolean updateIfNewer(PrivacyAuthorization authorization);
}
