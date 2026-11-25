package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleRelation;

import java.time.Instant;
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

    /**
     * 按关系ID查询（含 RELEASED 历史关系）
     *
     * @param relationId 关系ID
     * @return 关系
     */
    Optional<UserVehicleRelation> findByRelationId(String relationId);

    /**
     * 锁定读取关系（事务内使用，外部 RPC 不得在此之后执行）
     *
     * @param relationId 关系ID
     * @return 关系
     */
    Optional<UserVehicleRelation> lockByRelationId(String relationId);

    /**
     * 查询 VIN 最近一次已释放关系（作为历史信息，不代表当前授权）
     *
     * @param vin 车辆 VIN
     * @return 关系
     */
    Optional<UserVehicleRelation> findLatestReleasedByVin(String vin);

    /**
     * 条件释放关系（ACTIVE + 预期版本 → RELEASED）
     * <p>
     * 影响行数为并发最终判定；0 行表示关系已非 ACTIVE 或版本已变更。
     *
     * @param relationId          关系ID
     * @param expectedRowVersion  预期版本
     * @param releasedAt          释放绝对时刻
     * @param reason              解绑原因
     * @param operationId         解绑操作ID
     * @param releasedBy          可信操作人
     * @param now                 修改绝对时刻
     * @return 更新成功返回 true
     */
    boolean release(String relationId, int expectedRowVersion, Instant releasedAt, String reason,
                    String operationId, String releasedBy, Instant now);
}
