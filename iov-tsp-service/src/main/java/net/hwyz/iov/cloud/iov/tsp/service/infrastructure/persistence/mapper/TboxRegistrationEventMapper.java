package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxRegistrationEventPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * TBOX 注册激活事件消费幂等记录 DAO
 * </p>
 *
 * @author hwyz_leo
 */
@Mapper
public interface TboxRegistrationEventMapper {

    String COLUMNS = "id, event_id AS eventId, request_id AS requestId, device_sn AS deviceSn, "
            + "event_type AS eventType, occurred_at AS occurredAt, processed_at AS processedAt, "
            + "result, contract_digest AS contractDigest, create_time AS createTime";

    /**
     * 根据事件ID查询
     *
     * @param eventId 事件ID
     * @return 消费记录
     */
    @Select("SELECT " + COLUMNS + " FROM tb_tbox_registration_event WHERE event_id = #{eventId} LIMIT 1")
    TboxRegistrationEventPo selectByEventId(@Param("eventId") String eventId);

    /**
     * 根据事件类型与请求ID查询
     *
     * @param eventType 事件类型
     * @param requestId 请求ID
     * @return 消费记录
     */
    @Select("SELECT " + COLUMNS + " FROM tb_tbox_registration_event "
            + "WHERE event_type = #{eventType} AND request_id = #{requestId} LIMIT 1")
    TboxRegistrationEventPo selectByEventTypeAndRequestId(@Param("eventType") String eventType,
                                                          @Param("requestId") String requestId);

    /**
     * 插入消费记录
     *
     * @param po 消费记录
     * @return 影响行数
     */
    @Insert("INSERT INTO tb_tbox_registration_event "
            + "(event_id, request_id, device_sn, event_type, occurred_at, processed_at, result, contract_digest, create_time) "
            + "VALUES (#{eventId}, #{requestId}, #{deviceSn}, #{eventType}, #{occurredAt}, #{processedAt}, "
            + "#{result}, #{contractDigest}, #{createTime})")
    int insert(TboxRegistrationEventPo po);
}
