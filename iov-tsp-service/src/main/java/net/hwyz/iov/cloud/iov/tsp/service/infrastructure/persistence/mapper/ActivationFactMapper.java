package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.ActivationFactPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 首次激活候选事实与投递 Outbox DAO。
 *
 * @author hwyz_leo
 */
@Mapper
public interface ActivationFactMapper {

    String COLUMNS = "id, event_id AS eventId, schema_version AS schemaVersion, vin, "
            + "binding_operation_id AS bindingOperationId, source_fact_ref AS sourceFactRef, "
            + "occurred_at AS occurredAt, payload_digest AS payloadDigest, publish_status AS publishStatus, "
            + "retry_count AS retryCount, last_error AS lastError, published_at AS publishedAt, "
            + "row_version AS rowVersion, row_valid AS rowValid";

    /**
     * 按事件ID查询
     */
    @Select("SELECT " + COLUMNS + " FROM tb_user_vehicle_activation_fact WHERE event_id = #{eventId} LIMIT 1")
    ActivationFactPo selectByEventId(@Param("eventId") String eventId);

    /**
     * 按业务键查询（唯一候选事实）
     */
    @Select("SELECT " + COLUMNS + " FROM tb_user_vehicle_activation_fact "
            + "WHERE vin = #{vin} AND binding_operation_id = #{operationId} LIMIT 1")
    ActivationFactPo selectByBusinessKey(@Param("vin") String vin, @Param("operationId") String operationId);

    /**
     * 新增候选事实（与绑定事务同事务）
     */
    @Insert("INSERT INTO tb_user_vehicle_activation_fact (event_id, schema_version, vin, binding_operation_id, "
            + "source_fact_ref, occurred_at, payload_digest, publish_status, retry_count, row_version, row_valid) "
            + "VALUES (#{eventId}, #{schemaVersion}, #{vin}, #{bindingOperationId}, #{sourceFactRef}, "
            + "#{occurredAt}, #{payloadDigest}, #{publishStatus}, 0, 1, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ActivationFactPo po);

    /**
     * 查询待投递事实
     */
    @Select("SELECT " + COLUMNS + " FROM tb_user_vehicle_activation_fact "
            + "WHERE publish_status = 'PENDING' ORDER BY create_time LIMIT #{limit}")
    List<ActivationFactPo> selectPending(@Param("limit") int limit);

    /**
     * 标记投递成功
     */
    @Update("UPDATE tb_user_vehicle_activation_fact SET publish_status = 'PUBLISHED', last_error = NULL, "
            + "published_at = #{now}, modify_time = #{now}, row_version = row_version + 1 "
            + "WHERE event_id = #{eventId} AND publish_status = 'PENDING'")
    int markPublished(@Param("eventId") String eventId, @Param("now") LocalDateTime now);

    /**
     * 标记投递失败（保留可重试结果；达到阈值转 FAILED 终态）
     */
    @Update("UPDATE tb_user_vehicle_activation_fact SET publish_status = #{status}, "
            + "retry_count = retry_count + 1, last_error = #{error}, modify_time = #{now}, "
            + "row_version = row_version + 1 WHERE event_id = #{eventId} AND publish_status = 'PENDING'")
    int markFailed(@Param("eventId") String eventId, @Param("error") String error,
                   @Param("status") String status, @Param("now") LocalDateTime now);
}
