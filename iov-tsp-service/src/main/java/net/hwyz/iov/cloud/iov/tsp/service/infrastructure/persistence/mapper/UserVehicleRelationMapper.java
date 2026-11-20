package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.UserVehicleRelationPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户—车辆主使用人关系 DAO。
 * <p>
 * active_vin 生成列 + 唯一键是「每 VIN 至多一位当前主使用人」的并发最终防线。
 *
 * @author hwyz_leo
 */
@Mapper
public interface UserVehicleRelationMapper {

    String COLUMNS = "id, relation_id AS relationId, account_id AS accountId, vin, role, state, "
            + "bound_at AS boundAt, source_operation_id AS sourceOperationId, rule_version AS ruleVersion, "
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
}
