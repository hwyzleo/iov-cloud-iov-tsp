package net.hwyz.iov.cloud.iov.tsp.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MPT TBOX 异常标签结果
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TboxAnomalyTagResult {

    private net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxAnomalyType type;

    private java.time.LocalDateTime evaluatedAt;

    private java.time.LocalDateTime since;

    private Long durationSeconds;
}
