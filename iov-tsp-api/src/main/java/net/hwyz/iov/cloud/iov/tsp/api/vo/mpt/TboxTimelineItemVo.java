package net.hwyz.iov.cloud.iov.tsp.api.vo.mpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxTimelineMilestone;

import java.time.LocalDateTime;

/**
 * TBOX 生命周期时间线里程碑项
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TboxTimelineItemVo {

    /**
     * 里程碑
     */
    private TboxTimelineMilestone milestone;

    /**
     * 发生时间
     */
    private LocalDateTime occurredAt;

    /**
     * 来源引用（事件ID等），无来源为 null
     */
    private String sourceRef;

    /**
     * 说明
     */
    private String detail;
}
