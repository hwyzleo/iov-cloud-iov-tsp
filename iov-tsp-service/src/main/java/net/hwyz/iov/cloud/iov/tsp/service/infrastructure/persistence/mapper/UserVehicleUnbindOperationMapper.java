package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.UserVehicleUnbindOperationPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户车辆关系人工解绑与 CIAM 凭证撤销台账 DAO（IOV-TSP-DSN-CR-021）。
 * <p>
 * 所有状态推进使用条件更新与 row_version，撤销结果与关系状态分开表达。
 *
 * @author hwyz_leo
 */
@Mapper
public interface UserVehicleUnbindOperationMapper {

    String COLUMNS = "id, operation_id AS operationId, relation_id AS relationId, account_id AS accountId, "
            + "vin, operator_id AS operatorId, idempotency_key AS idempotencyKey, "
            + "request_digest AS requestDigest, expected_row_version AS expectedRowVersion, reason, "
            + "relation_state AS relationState, released_at AS releasedAt, completed_at AS completedAt, "
            + "credential_revocation_status AS credentialRevocationStatus, "
            + "revoked_session_count AS revokedSessionCount, retry_count AS retryCount, "
            + "next_retry_at AS nextRetryAt, last_error AS lastError, row_version AS rowVersion";

    /**
     * 新增解绑操作
     *
     * @param po 操作
     * @return 影响行数
     */
    @Insert("INSERT INTO tb_user_vehicle_unbind_operation (operation_id, relation_id, account_id, vin, "
            + "operator_id, idempotency_key, request_digest, expected_row_version, reason, relation_state, "
            + "credential_revocation_status, retry_count, row_version, row_valid) "
            + "VALUES (#{operationId}, #{relationId}, #{accountId}, #{vin}, #{operatorId}, #{idempotencyKey}, "
            + "#{requestDigest}, #{expectedRowVersion}, #{reason}, #{relationState}, #{credentialRevocationStatus}, "
            + "0, 1, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserVehicleUnbindOperationPo po);

    /**
     * 按操作ID查询
     *
     * @param operationId 操作ID
     * @return 操作
     */
    @Select("SELECT " + COLUMNS + " FROM tb_user_vehicle_unbind_operation "
            + "WHERE operation_id = #{operationId} AND row_valid = 1 LIMIT 1")
    UserVehicleUnbindOperationPo selectByOperationId(@Param("operationId") String operationId);

    /**
     * 按操作人与幂等键查询
     *
     * @param operatorId     操作人
     * @param idempotencyKey 幂等键
     * @return 操作
     */
    @Select("SELECT " + COLUMNS + " FROM tb_user_vehicle_unbind_operation "
            + "WHERE operator_id = #{operatorId} AND idempotency_key = #{idempotencyKey} AND row_valid = 1 LIMIT 1")
    UserVehicleUnbindOperationPo selectByOperatorAndKey(@Param("operatorId") String operatorId,
                                                        @Param("idempotencyKey") String idempotencyKey);

    /**
     * 标记关系已释放（PENDING → RELEASED）
     */
    @Update("UPDATE tb_user_vehicle_unbind_operation SET relation_state = 'RELEASED', released_at = #{releasedAt}, "
            + "completed_at = #{completedAt}, next_retry_at = #{nextRetryAt}, modify_time = #{now}, "
            + "row_version = row_version + 1 "
            + "WHERE operation_id = #{operationId} AND relation_state = 'PENDING' "
            + "AND row_version = #{expectedVersion} AND row_valid = 1")
    int markReleased(@Param("operationId") String operationId,
                     @Param("releasedAt") LocalDateTime releasedAt,
                     @Param("completedAt") LocalDateTime completedAt,
                     @Param("nextRetryAt") LocalDateTime nextRetryAt,
                     @Param("now") LocalDateTime now,
                     @Param("expectedVersion") int expectedVersion);

    /**
     * 标记撤销成功（RELEASED + 非 SUCCEEDED → SUCCEEDED）
     */
    @Update("UPDATE tb_user_vehicle_unbind_operation SET credential_revocation_status = 'SUCCEEDED', "
            + "revoked_session_count = #{revokedSessionCount}, next_retry_at = NULL, last_error = NULL, "
            + "modify_time = #{now}, row_version = row_version + 1 "
            + "WHERE operation_id = #{operationId} AND relation_state = 'RELEASED' "
            + "AND credential_revocation_status IN ('PENDING', 'FAILED') "
            + "AND row_version = #{expectedVersion} AND row_valid = 1")
    int markRevocationSucceeded(@Param("operationId") String operationId,
                                @Param("revokedSessionCount") int revokedSessionCount,
                                @Param("now") LocalDateTime now,
                                @Param("expectedVersion") int expectedVersion);

    /**
     * 标记撤销失败（RELEASED + 非 SUCCEEDED → FAILED）
     */
    @Update("UPDATE tb_user_vehicle_unbind_operation SET credential_revocation_status = 'FAILED', "
            + "last_error = #{lastError}, next_retry_at = #{nextRetryAt}, "
            + "retry_count = retry_count + #{incrementRetry}, modify_time = #{now}, row_version = row_version + 1 "
            + "WHERE operation_id = #{operationId} AND relation_state = 'RELEASED' "
            + "AND credential_revocation_status IN ('PENDING', 'FAILED') "
            + "AND row_version = #{expectedVersion} AND row_valid = 1")
    int markRevocationFailed(@Param("operationId") String operationId,
                             @Param("lastError") String lastError,
                             @Param("nextRetryAt") LocalDateTime nextRetryAt,
                             @Param("incrementRetry") int incrementRetry,
                             @Param("now") LocalDateTime now,
                             @Param("expectedVersion") int expectedVersion);

    /**
     * 查询待自动重试的撤销操作
     */
    @Select("SELECT " + COLUMNS + " FROM tb_user_vehicle_unbind_operation "
            + "WHERE row_valid = 1 AND relation_state = 'RELEASED' "
            + "AND credential_revocation_status IN ('PENDING', 'FAILED') "
            + "AND next_retry_at IS NOT NULL AND next_retry_at <= #{now} "
            + "AND retry_count < #{maxRetryCount} "
            + "ORDER BY next_retry_at ASC, id ASC LIMIT #{limit}")
    List<UserVehicleUnbindOperationPo> selectRevocationRetryCandidates(@Param("now") LocalDateTime now,
                                                                       @Param("maxRetryCount") int maxRetryCount,
                                                                       @Param("limit") int limit);
}
