package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleRelation;

import java.util.Optional;

/**
 * 用户—车辆主使用人关系仓储。
 *
 * @author hwyz_leo
 */
public interface UserVehicleRelationRepository {

    /**
     * 新增关系（唯一键为并发最终防线）
     *
     * @param relation 关系
     */
    void insert(UserVehicleRelation relation);

    /**
     * 查询 VIN 当前主使用人
     *
     * @param vin 车辆 VIN
     * @return 关系
     */
    Optional<UserVehicleRelation> findActiveByVin(String vin);

    /**
     * 查询账号在该 VIN 上的当前关系
     *
     * @param accountId 账号
     * @param vin       车辆 VIN
     * @return 关系
     */
    Optional<UserVehicleRelation> findActiveByAccountAndVin(String accountId, String vin);
}
