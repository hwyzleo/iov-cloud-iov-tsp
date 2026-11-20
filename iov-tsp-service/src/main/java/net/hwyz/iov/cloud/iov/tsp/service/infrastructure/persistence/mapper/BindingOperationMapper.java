package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.BindingOperationPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 绑定确认操作 DAO（幂等台账）。
 *
 * @author hwyz_leo
 */
@Mapper
public interface BindingOperationMapper {

    String COLUMNS = "id, operation_id AS operationId, caller_scope AS callerScope, account_id AS accountId, "
            + "idempotency_key AS idempotencyKey, request_digest AS requestDigest, session_id AS sessionId, vin, "
            + "status, result_reason AS resultReason, result_relation_id AS resultRelationId, eligibility, "
            + "rule_version AS ruleVersion, fact_ref AS factRef, fact_checked_at AS factCheckedAt, "
            + "completed_at AS completedAt, row_version AS rowVersion, row_valid AS rowValid";

    /**
     * 新增操作
     *
     * @param po 操作
     * @return 影响行数
     */
    @Insert("INSERT INTO tb_user_vehicle_binding_operation (operation_id, caller_scope, account_id, "
            + "idempotency_key, request_digest, session_id, vin, status, eligibility, rule_version, fact_ref, "
            + "fact_checked_at, row_version, row_valid) "
            + "VALUES (#{operationId}, #{callerScope}, #{accountId}, #{idempotencyKey}, #{requestDigest}, "
            + "#{sessionId}, #{vin}, #{status}, #{eligibility}, #{ruleVersion}, #{factRef}, #{factCheckedAt}, 1, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BindingOperationPo po);

    /**
     * 按操作ID查询
     *
     * @param operationId 操作ID
     * @return 操作
     */
    @Select("SELECT " + COLUMNS + " FROM tb_user_vehicle_binding_operation "
            + "WHERE operation_id = #{operationId} AND row_valid = 1 LIMIT 1")
    BindingOperationPo selectByOperationId(@Param("operationId") String operationId);

    /**
     * 按操作ID锁定读取
     *
     * @param operationId 操作ID
     * @return 操作
     */
    @Select("SELECT " + COLUMNS + " FROM tb_user_vehicle_binding_operation "
            + "WHERE operation_id = #{operationId} AND row_valid = 1 LIMIT 1 FOR UPDATE")
    BindingOperationPo selectByOperationIdForUpdate(@Param("operationId") String operationId);

    /**
     * 按幂等键查询
     *
     * @param callerScope    调用方范围
     * @param accountId      账号
     * @param idempotencyKey 幂等键
     * @return 操作
     */
    @Select("SELECT " + COLUMNS + " FROM tb_user_vehicle_binding_operation "
            + "WHERE caller_scope = #{callerScope} AND account_id = #{accountId} "
            + "AND idempotency_key = #{idempotencyKey} AND row_valid = 1 LIMIT 1")
    BindingOperationPo selectByIdempotencyKey(@Param("callerScope") String callerScope,
                                              @Param("accountId") String accountId,
                                              @Param("idempotencyKey") String idempotencyKey);

    /**
     * 按幂等键锁定读取
     *
     * @param callerScope    调用方范围
     * @param accountId      账号
     * @param idempotencyKey 幂等键
     * @return 操作
     */
    @Select("SELECT " + COLUMNS + " FROM tb_user_vehicle_binding_operation "
            + "WHERE caller_scope = #{callerScope} AND account_id = #{accountId} "
            + "AND idempotency_key = #{idempotencyKey} AND row_valid = 1 LIMIT 1 FOR UPDATE")
    BindingOperationPo selectByIdempotencyKeyForUpdate(@Param("callerScope") String callerScope,
                                                       @Param("accountId") String accountId,
                                                       @Param("idempotencyKey") String idempotencyKey);

    /**
     * 标记成功
     */
    @Update("UPDATE tb_user_vehicle_binding_operation SET status = 'SUCCEEDED', result_reason = NULL, "
            + "result_relation_id = #{relationId}, fact_ref = #{factRef}, fact_checked_at = #{factCheckedAt}, "
            + "completed_at = #{now}, modify_time = #{now}, row_version = row_version + 1 "
            + "WHERE operation_id = #{operationId} AND status = 'PENDING' AND row_version = #{expectedVersion}")
    int markSucceeded(@Param("operationId") String operationId, @Param("relationId") String relationId,
                      @Param("factRef") String factRef, @Param("factCheckedAt") LocalDateTime factCheckedAt,
                      @Param("now") LocalDateTime now, @Param("expectedVersion") int expectedVersion);

    /**
     * 标记失败
     */
    @Update("UPDATE tb_user_vehicle_binding_operation SET status = 'FAILED', result_reason = #{reason}, "
            + "eligibility = #{eligibility}, rule_version = #{ruleVersion}, fact_ref = #{factRef}, "
            + "fact_checked_at = #{factCheckedAt}, completed_at = #{now}, modify_time = #{now}, "
            + "row_version = row_version + 1 "
            + "WHERE operation_id = #{operationId} AND status = 'PENDING' AND row_version = #{expectedVersion}")
    int markFailed(@Param("operationId") String operationId, @Param("reason") String reason,
                   @Param("eligibility") String eligibility, @Param("ruleVersion") String ruleVersion,
                   @Param("factRef") String factRef, @Param("factCheckedAt") LocalDateTime factCheckedAt,
                   @Param("now") LocalDateTime now, @Param("expectedVersion") int expectedVersion);
}
