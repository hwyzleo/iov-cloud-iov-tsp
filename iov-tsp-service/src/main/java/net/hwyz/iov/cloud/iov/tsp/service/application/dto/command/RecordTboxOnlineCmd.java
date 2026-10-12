package net.hwyz.iov.cloud.iov.tsp.service.application.dto.command;

import java.time.Instant;

/**
 * 记录 TBOX 上线的内部命令
 * <p>
 * TSP-DSN-CR-010 §3.2：Adapter 完成防腐转换与契约校验后，只向应用层传递内部命令对象，
 * 不使用通用 Map/JSON 节点传播未校验字段。hsmUid 已规范化为 trim+uppercase。
 *
 * @param eventId              事件唯一标识（幂等身份）
 * @param hsmUid               规范化设备身份
 * @param vin                  上报车架号（可选，交叉校验）
 * @param tboxSn               上报 TBOX 序列号（可选，交叉校验）
 * @param sessionId            会话ID
 * @param connectionGeneration VAGW 连接代际
 * @param occurredAt           事件发生时间（已校验）
 * @param timeSource           occurredAt 来源
 * @param sourceTopic          来源 Kafka Topic（用于事件事实落库）
 * @author hwyz_leo
 */
public record RecordTboxOnlineCmd(
        String eventId,
        String hsmUid,
        String vin,
        String tboxSn,
        String sessionId,
        long connectionGeneration,
        Instant occurredAt,
        String timeSource,
        String sourceTopic) {
}
