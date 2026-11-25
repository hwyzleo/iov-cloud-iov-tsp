package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.UserVehicleRelationPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 用户—车辆主使用人关系 DAO。
 * <p>
 * active_vin 生成列 + 唯一键是「每 VIN 至多一位当前主使用人」的并发最终防线；
 * IOV-TSP-DSN-CR-021：释放使用「ACTIVE + 预期版本」条件更新，历史行不删除。
 *
 * @author hwyz_leo
 */
@Mapper
public interface UserVehicleRelationMapper {

    String COLUMNS = "id, relation_id AS relationId, account_id AS accountId, vin, role, state, "
            + "bound_at AS boundAt, released_at AS releasedAt, release_reason AS releaseReason, "
            + "release_operation_id AS releaseOperationId, released_by AS releasedBy, "
            + "source_operation_id AS sourceOperationId, rule_version AS ruleVersion, "
            + "fact_ref AS factRef, row_version AS rowVersion, row_valid AS rowValid";

    /**
     * 新增关系
     *
     * @param po 关系
     * @return 影响行数
     */
    @Insert("INSERT INTO tb_user_vehicle_relation (relation_id, account_id, vin, role, state, bound_at, "
            + "source_operation_id, rule_version, fact_ref, row_version, row_valid) "
            + "VALUES (#{relationId}, #{accountId}, #{vin}, #{role}, #{state}, #{boundAt}, "
            + "#{sourceOperationId}, #{ruleVersion}, #{factRef}, 1, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserVehicleRelationPo po);

    /**
     * 查询 VIN 当前主使用人
     *
     * @param vin 车辆 VIN
     * @return 关系
     */
    @Select("SELECT " + COLUMNS + " FROM tb_user_vehicle_relation "
            + "WHERE vin = #{vin} AND state = 'ACTIVE' AND row_valid = 1 LIMIT 1")
    UserVehicleRelationPo selectActiveByVin(@Param("vin") String vin);

    /**
     * 查询账号在该 VIN 上的当前关系
     *
     * @param accountId 账号
     * @param vin       车辆 VIN
     * @return 关系
     */
    @Select("SELECT " + COLUMNS + " FROM tb_user_vehicle_relation "
            + "WHERE account_id = #{accountId} AND vin = #{vin} AND state = 'ACTIVE' AND row_valid = 1 LIMIT 1")
    UserVehicleRelationPo selectActiveByAccountAndVin(@Param("accountId") String accountId, @Param("vin") String vin);

    /**
     * 按关系ID查询（含 RELEASED 历史关系）
     *
     * @param relationId 关系ID
     * @return 关系
     */
    @Select("SELECT " + COLUMNS + " FROM tb_user_vehicle_relation "
            + "WHERE relation_id = #{relationId} AND row_valid = 1 LIMIT 1")
    UserVehicleRelationPo selectByRelationId(@Param("relationId") String relationId);

    /**
     * 锁定读取关系（事务内使用）
     *
     * @param relationId 关系ID
     * @return 关系
     */
    @Select("SELECT " + COLUMNS + " FROM tb_user_vehicle_relation "
            + "WHERE relation_id = #{relationId} AND row_valid = 1 FOR UPDATE")
    UserVehicleRelationPo lockByRelationId(@Param("relationId") String relationId);

    /**
     * 查询 VIN 最近一次已释放关系
     *
     * @param vin 车辆 VIN
     * @return 关系
     */
    @Select("SELECT " + COLUMNS + " FROM tb_user_vehicle_relation "
            + "WHERE vin = #{vin} AND state = 'RELEASED' AND row_valid = 1 "
            + "ORDER BY released_at DESC, id DESC LIMIT 1")
    UserVehicleRelationPo selectLatestReleasedByVin(@Param("vin") String vin);

    /**
     * 条件释放关系（ACTIVE + 预期版本 → RELEASED）
     *
     * @param relationId         关系ID
     * @param expectedRowVersion 预期版本
     * @param releasedAt         释放绝对时刻（UTC 墙钟）
     * @param reason             解绑原因
     * @param operationId        解绑操作ID
     * @param releasedBy         可信操作人
     * @param now                修改绝对时刻（UTC 墙钟）
     * @return 影响行数
     */
    @Update("UPDATE tb_user_vehicle_relation SET state = 'RELEASED', released_at = #{releasedAt}, "
            + "release_reason = #{reason}, release_operation_id = #{operationId}, released_by = #{releasedBy}, "
            + "modify_time = #{now}, modify_by = #{releasedBy}, row_version = row_version + 1 "
            + "WHERE relation_id = #{relationId} AND state = 'ACTIVE' AND row_version = #{expectedRowVersion} "
            + "AND row_valid = 1")
    int release(@Param("relationId") String relationId,
                @Param("expectedRowVersion") int expectedRowVersion,
                @Param("releasedAt") LocalDateTime releasedAt,
                @Param("reason") String reason,
                @Param("operationId") String operationId,
                @Param("releasedBy") String releasedBy,
                @Param("now") LocalDateTime now);
}
