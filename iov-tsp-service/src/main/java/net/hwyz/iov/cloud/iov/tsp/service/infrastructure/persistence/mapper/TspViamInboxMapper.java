package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TspViamInboxPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * TSP 消费 VIAM 安全事件幂等 Inbox DAO（IOV-TSP-DSN-CR-016）
 * </p>
 *
 * @author hwyz_leo
 */
@Mapper
public interface TspViamInboxMapper {

    String COLUMNS = "id, event_id AS eventId, topic, event_type AS eventType, event_key AS eventKey, "
            + "principal_id AS principalId, hsm_uid AS hsmUid, device_sn AS deviceSn, "
            + "contract_digest AS contractDigest, status, retry_count AS retryCount, error, "
            + "received_at AS receivedAt, processed_at AS processedAt, create_time AS createTime, "
            + "modify_time AS modifyTime";

    /**
     * 根据事件ID查询（幂等键）
     *
     * @param eventId 事件ID
     * @return Inbox 记录
     */
    @Select("SELECT " + COLUMNS + " FROM tb_tsp_viam_inbox WHERE event_id = #{eventId} LIMIT 1")
    TspViamInboxPo selectByEventId(@Param("eventId") String eventId);

    /**
     * 插入消费记录（同 eventId 由唯一键约束防重，调用方捕获冲突）
     *
     * @param po Inbox 记录
     * @return 影响行数
     */
    @Insert("INSERT INTO tb_tsp_viam_inbox "
            + "(event_id, topic, event_type, event_key, principal_id, hsm_uid, device_sn, contract_digest, "
            + "status, retry_count, error, received_at, processed_at, create_time) "
            + "VALUES (#{eventId}, #{topic}, #{eventType}, #{eventKey}, #{principalId}, #{hsmUid}, #{deviceSn}, "
            + "#{contractDigest}, #{status}, #{retryCount}, #{error}, #{receivedAt}, #{processedAt}, #{createTime})")
    int insert(TspViamInboxPo po);

    /**
     * 标记处理完成（幂等收敛）
     *
     * @param eventId     事件ID
     * @param processedAt 处理完成时间
     * @return 影响行数
     */
    @Update("UPDATE tb_tsp_viam_inbox SET status = 'PROCESSED', processed_at = #{processedAt}, error = NULL "
            + "WHERE event_id = #{eventId}")
    int markProcessed(@Param("eventId") String eventId, @Param("processedAt") java.time.LocalDateTime processedAt);

    /**
     * 标记可重试/对账（资产未导入等）；retry_count 递增
     *
     * @param eventId 事件ID
     * @param error   脱敏错误摘要
     * @return 影响行数
     */
    @Update("UPDATE tb_tsp_viam_inbox SET status = 'RETRYABLE', retry_count = retry_count + 1, error = #{error} "
            + "WHERE event_id = #{eventId}")
    int markRetryable(@Param("eventId") String eventId, @Param("error") String error);

    /**
     * 标记契约冲突（同 eventId 不同内容，不静默当重复成功）
     *
     * @param eventId 事件ID
     * @param error   冲突摘要
     * @return 影响行数
     */
    @Update("UPDATE tb_tsp_viam_inbox SET status = 'CONFLICT', error = #{error} WHERE event_id = #{eventId}")
    int markConflict(@Param("eventId") String eventId, @Param("error") String error);

    /**
     * 标记处理失败（重试耗尽；错误可见、可重放）
     *
     * @param eventId 事件ID
     * @param error   脱敏错误摘要
     * @return 影响行数
     */
    @Update("UPDATE tb_tsp_viam_inbox SET status = 'FAILED', retry_count = retry_count + 1, error = #{error} "
            + "WHERE event_id = #{eventId}")
    int markFailed(@Param("eventId") String eventId, @Param("error") String error);
}
