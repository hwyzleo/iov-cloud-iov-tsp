package net.hwyz.iov.cloud.iov.tsp.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxTimelineMilestone;

import java.time.LocalDateTime;

/**
 * MPT TBOX 时间线里程碑结果
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TboxTimelineItemResult {

    private TboxTimelineMilestone milestone;

    private LocalDateTime occurredAt;

    private String sourceRef;

    private String detail;
}
