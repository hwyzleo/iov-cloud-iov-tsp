package net.hwyz.iov.cloud.iov.tsp.api.vo.mpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * TBOX 生命周期时间线
 * <p>
 * partial=true 表示消费幂等记录（Inbox）不可用，仅返回由设备字段可证实的时间线。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptTboxTimelineVo {

    /**
     * 是否为部分时间线（Inbox 不可用）
     */
    private boolean partial;

    /**
     * 里程碑列表（按时间升序）
     */
    private List<TboxTimelineItemVo> milestones;
}
