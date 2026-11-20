package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.PrivacyAuthorization;

import java.util.Optional;

/**
 * Account + VIN 隐私授权仓储。
 *
 * @author hwyz_leo
 */
public interface PrivacyAuthorizationRepository {

    /**
     * 查询当前状态
     *
     * @param accountId         账号
     * @param vin               车辆 VIN
     * @param authorizationItem 授权项
     * @return 授权记录
     */
    Optional<PrivacyAuthorization> find(String accountId, String vin, String authorizationItem);

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
