package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.ActivationSessionPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 一次性激活会话 DAO（IOV-TSP-DSN-CR-017）。
 * <p>
 * 所有写操作使用状态 + 版本条件更新，并以数据库时钟无关的服务端时刻再验到期。
 *
 * @author hwyz_leo
 */
@Mapper
public interface ActivationSessionMapper {

    String COLUMNS = "id, session_id AS sessionId, qr_token_hash AS qrTokenHash, "
            + "trusted_vehicle_ref AS trustedVehicleRef, vin, agreement_revisions AS agreementRevisions, "
            + "expires_at AS expiresAt, state, claimed_account_id AS claimedAccountId, claimed_at AS claimedAt, "
            + "binding_operation_id AS bindingOperationId, skipped_at AS skippedAt, cancelled_at AS cancelledAt, "
            + "bound_at AS boundAt, result_expires_at AS resultExpiresAt, ivi_login_state AS iviLoginState, "
            + "ivi_login_report_id AS iviLoginReportId, ivi_login_reported_at AS iviLoginReportedAt, "
            + "row_version AS rowVersion, row_valid AS rowValid";

    /**
     * 新增会话
     *
     * @param po 会话
     * @return 影响行数
     */
    @Insert("INSERT INTO tb_activation_session (session_id, qr_token_hash, trusted_vehicle_ref, vin, "
            + "agreement_revisions, expires_at, state, claimed_account_id, claimed_at, binding_operation_id, "
            + "result_expires_at, ivi_login_state, row_version, row_valid) "
            + "VALUES (#{sessionId}, #{qrTokenHash}, #{trustedVehicleRef}, #{vin}, #{agreementRevisions}, "
            + "#{expiresAt}, #{state}, #{claimedAccountId}, #{claimedAt}, #{bindingOperationId}, "
            + "#{resultExpiresAt}, #{iviLoginState}, 1, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ActivationSessionPo po);

    /**
     * 按会话业务ID查询
     *
     * @param sessionId 会话业务ID
     * @return 会话
     */
    @Select("SELECT " + COLUMNS + " FROM tb_activation_session WHERE session_id = #{sessionId} AND row_valid = 1 LIMIT 1")
    ActivationSessionPo selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 按会话业务ID锁定读取（事务内，外部 RPC 之前不得调用）
     *
     * @param sessionId 会话业务ID
     * @return 会话
     */
    @Select("SELECT " + COLUMNS + " FROM tb_activation_session WHERE session_id = #{sessionId} AND row_valid = 1 LIMIT 1 FOR UPDATE")
    ActivationSessionPo selectBySessionIdForUpdate(@Param("sessionId") String sessionId);

    /**
     * 按二维码摘要查询
     *
     * @param qrTokenHash 二维码摘要
     * @return 会话
     */
    @Select("SELECT " + COLUMNS + " FROM tb_activation_session WHERE qr_token_hash = #{qrTokenHash} AND row_valid = 1 LIMIT 1")
    ActivationSessionPo selectByQrTokenHash(@Param("qrTokenHash") String qrTokenHash);

    /**
     * 条件认领账号（ISSUED → SCANNED）
     */
    @Update("UPDATE tb_activation_session SET state = 'SCANNED', claimed_account_id = #{accountId}, "
            + "claimed_at = #{now}, modify_time = #{now}, row_version = row_version + 1 "
            + "WHERE session_id = #{sessionId} AND state = 'ISSUED' AND row_version = #{expectedVersion} "
            + "AND row_valid = 1 AND expires_at >= #{now}")
    int claim(@Param("sessionId") String sessionId, @Param("accountId") String accountId,
              @Param("now") LocalDateTime now, @Param("expectedVersion") int expectedVersion);

    /**
     * 条件取消（ISSUED/SCANNED → CANCELLED）
     */
    @Update("UPDATE tb_activation_session SET state = 'CANCELLED', cancelled_at = #{now}, modify_time = #{now}, "
            + "row_version = row_version + 1 "
            + "WHERE session_id = #{sessionId} AND state IN ('ISSUED', 'SCANNED') "
            + "AND (#{accountId} IS NULL OR claimed_account_id IS NULL OR claimed_account_id = #{accountId}) "
            + "AND row_version = #{expectedVersion} AND row_valid = 1")
    int cancel(@Param("sessionId") String sessionId, @Param("accountId") String accountId,
               @Param("now") LocalDateTime now, @Param("expectedVersion") int expectedVersion);

    /**
     * 条件标记绑定完成（SCANNED → BOUND，确认前过期不得提交）
     */
    @Update("UPDATE tb_activation_session SET state = 'BOUND', binding_operation_id = #{operationId}, "
            + "bound_at = #{now}, result_expires_at = #{resultExpiresAt}, modify_time = #{now}, "
            + "row_version = row_version + 1 "
            + "WHERE session_id = #{sessionId} AND state = 'SCANNED' AND row_version = #{expectedVersion} "
            + "AND row_valid = 1 AND expires_at >= #{now}")
    int markBound(@Param("sessionId") String sessionId, @Param("operationId") String operationId,
                  @Param("now") LocalDateTime now, @Param("resultExpiresAt") LocalDateTime resultExpiresAt,
                  @Param("expectedVersion") int expectedVersion);

    /**
     * 条件标记跳过（ISSUED/SCANNED → SKIPPED）
     */
    @Update("UPDATE tb_activation_session SET state = 'SKIPPED', skipped_at = #{now}, modify_time = #{now}, "
            + "row_version = row_version + 1 "
            + "WHERE session_id = #{sessionId} AND state IN ('ISSUED', 'SCANNED') "
            + "AND row_version = #{expectedVersion} AND row_valid = 1")
    int markSkipped(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now,
                    @Param("expectedVersion") int expectedVersion);

    /**
     * 记录车机登录结果（仅已绑定会话；登录失败不改变关系）
     */
    @Update("UPDATE tb_activation_session SET ivi_login_state = #{status}, ivi_login_report_id = #{reportId}, "
            + "ivi_login_reported_at = #{now}, modify_time = #{now}, row_version = row_version + 1 "
            + "WHERE session_id = #{sessionId} AND state = 'BOUND' AND row_valid = 1")
    int reportIviLoginResult(@Param("sessionId") String sessionId, @Param("status") String status,
                             @Param("reportId") String reportId, @Param("now") LocalDateTime now);
}
