package net.hwyz.iov.cloud.iov.tsp.api.vo.mpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MPT TBOX 运营详情
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptTboxDetailVo {

    /**
     * 摘要信息（与列表项一致）
     */
    private MptTboxSummaryVo summary;

    private String hardwareNo;

    private String softwareNo;

    private String configWord;

    private String vehicleNodeCode;

    private Long bindingId;

    /**
     * 绑定状态：ACTIVE/INACTIVE（原始投影值，绑定冲突时为 null）
     */
    private String bindingState;

    /**
     * 绑定最后同步时间
     */
    private LocalDateTime bindingUpdatedAt;

    /**
     * 只读激活里程碑
     */
    private List<TboxTimelineItemVo> activationMilestones;

    /**
     * 页面联动稳定标识
     */
    private TboxLinkKeysVo linkKeys;
}
