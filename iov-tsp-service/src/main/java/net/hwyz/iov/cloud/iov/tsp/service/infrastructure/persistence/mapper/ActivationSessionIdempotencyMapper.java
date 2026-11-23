package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.ActivationSessionIdempotencyPo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 车端会话创建幂等台账 DAO（IOV-TSP-DSN-CR-020 D020-04）。
 * <p>
 * 唯一键 {@code uk_asi_scope_vin_key (caller_scope, vin, idempotency_key)} 为并发最终防线；
 * 过期释放使用物理删除（逻辑删除会让唯一键继续占用，导致同键无法复用）。
 *
 * @author hwyz_leo
 */
@Mapper
public interface ActivationSessionIdempotencyMapper {

    String COLUMNS = "id, idempotency_id AS idempotencyId, caller_scope AS callerScope, vin, "
            + "idempotency_key AS idempotencyKey, request_digest AS requestDigest, session_id AS sessionId, "
            + "expires_at AS expiresAt, row_version AS rowVersion, row_valid AS rowValid";

    /**
     * 新增台账
     *
     * @param po 台账
     * @return 影响行数
     */
    @Insert("INSERT INTO tb_activation_session_idempotency (idempotency_id, caller_scope, vin, idempotency_key, "
            + "request_digest, session_id, expires_at, row_version, row_valid) "
            + "VALUES (#{idempotencyId}, #{callerScope}, #{vin}, #{idempotencyKey}, #{requestDigest}, "
            + "#{sessionId}, #{expiresAt}, 1, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ActivationSessionIdempotencyPo po);

    /**
     * 按幂等作用域查询
     *
     * @param callerScope    调用方作用域
     * @param vin            车辆 VIN
     * @param idempotencyKey 幂等键
     * @return 台账
     */
    @Select("SELECT " + COLUMNS + " FROM tb_activation_session_idempotency "
            + "WHERE caller_scope = #{callerScope} AND vin = #{vin} AND idempotency_key = #{idempotencyKey} "
            + "AND row_valid = 1 LIMIT 1")
    ActivationSessionIdempotencyPo select(@Param("callerScope") String callerScope, @Param("vin") String vin,
                                          @Param("idempotencyKey") String idempotencyKey);

    /**
     * 物理删除已释放台账（同键可复用）
     *
     * @param id 主键
     * @return 影响行数
     */
    @Delete("DELETE FROM tb_activation_session_idempotency WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
