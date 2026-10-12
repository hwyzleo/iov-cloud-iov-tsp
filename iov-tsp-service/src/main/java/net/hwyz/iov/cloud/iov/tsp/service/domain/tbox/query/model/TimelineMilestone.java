package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query.model;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxTimelineMilestone;

import java.time.LocalDateTime;

/**
 * 生命周期时间线里程碑（领域模型）
 *
 * @param milestone  里程碑
 * @param occurredAt 发生时间
 * @param sourceRef  来源引用（事件ID等），无来源为 null
 * @param detail     说明
 * @author hwyz_leo
 */
public record TimelineMilestone(
        TboxTimelineMilestone milestone,
        LocalDateTime occurredAt,
        String sourceRef,
        String detail) {
}
