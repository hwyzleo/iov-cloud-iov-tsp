package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query.model;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxAnomalyType;

import java.time.LocalDateTime;

/**
 * 派生异常标签（领域模型）
 *
 * @param type            异常类型
 * @param evaluatedAt     判定时间
 * @param since           起算时间，未知为 null
 * @param durationSeconds 持续时长（秒），未知为 null
 * @author hwyz_leo
 */
public record AnomalyTag(
        TboxAnomalyType type,
        LocalDateTime evaluatedAt,
        LocalDateTime since,
        Long durationSeconds) {
}
