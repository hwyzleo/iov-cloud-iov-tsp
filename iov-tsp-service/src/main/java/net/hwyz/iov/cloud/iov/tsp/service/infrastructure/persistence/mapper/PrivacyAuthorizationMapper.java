package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.AccountVehiclePrivacyAuthorizationPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Account + VIN 隐私授权 DAO。
 *
 * @author hwyz_leo
 */
@Mapper
public interface PrivacyAuthorizationMapper {

    String COLUMNS = "id, authorization_id AS authorizationId, account_id AS accountId, vin, "
            + "authorization_item AS authorizationItem, state, version, source, report_id AS reportId, "
            + "payload_digest AS payloadDigest, reported_at AS reportedAt, received_at AS receivedAt";

    /**
     * 查询当前状态
     */
    @Select("SELECT " + COLUMNS + " FROM tb_account_vehicle_privacy_authorization "
            + "WHERE account_id = #{accountId} AND vin = #{vin} AND authorization_item = #{item} "
            + "AND row_valid = 1 LIMIT 1")
    AccountVehiclePrivacyAuthorizationPo select(@Param("accountId") String accountId, @Param("vin") String vin,
                                                @Param("item") String item);

    /**
     * 新增授权记录
     */
    @Insert("INSERT INTO tb_account_vehicle_privacy_authorization (authorization_id, account_id, vin, "
            + "authorization_item, state, version, source, report_id, payload_digest, reported_at, received_at, "
            + "row_version, row_valid) "
            + "VALUES (#{authorizationId}, #{accountId}, #{vin}, #{authorizationItem}, #{state}, #{version}, "
            + "#{source}, #{reportId}, #{payloadDigest}, #{reportedAt}, #{receivedAt}, 1, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AccountVehiclePrivacyAuthorizationPo po);

    /**
     * 仅当来源版本更新时覆盖（旧版本/乱序不得覆盖新状态）
     */
    @Update("UPDATE tb_account_vehicle_privacy_authorization SET state = #{state}, version = #{version}, "
            + "source = #{source}, report_id = #{reportId}, payload_digest = #{payloadDigest}, "
            + "reported_at = #{reportedAt}, received_at = #{receivedAt}, modify_time = #{receivedAt}, "
            + "row_version = row_version + 1 "
            + "WHERE account_id = #{accountId} AND vin = #{vin} AND authorization_item = #{authorizationItem} "
            + "AND version < #{version} AND row_valid = 1")
    int updateIfNewer(AccountVehiclePrivacyAuthorizationPo po);
}
