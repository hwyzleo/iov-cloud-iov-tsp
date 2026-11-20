package net.hwyz.iov.cloud.iov.tsp.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车端回执处理结果。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportAckVo {

    /**
     * 回执ID
     */
    private String reportId;

    /**
     * 是否已被接受（重复同内容同样视为接受）
     */
    private Boolean accepted;
}
