package net.hwyz.iov.cloud.iov.tsp.api.vo.mpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxAnomalyType;

import java.time.LocalDateTime;

/**
 * TBOX 派生异常标签
 * <p>
 * 标签为查询时派生，返回判定时间、起算时间与持续时长；无权威时间来源时对应字段为 null。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TboxAnomalyTagVo {

    /**
     * 异常类型
     */
    private TboxAnomalyType type;

    /**
     * 判定时间（服务端当前时间）
     */
    private LocalDateTime evaluatedAt;

    /**
     * 异常起算时间，未知为 null
     */
    private LocalDateTime since;

    /**
     * 持续时长（秒），未知为 null
     */
    private Long durationSeconds;
}
