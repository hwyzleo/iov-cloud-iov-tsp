package net.hwyz.iov.cloud.iov.tsp.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MPT TBOX 运营详情结果
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptTboxDetailResult {

    private MptTboxSummaryResult summary;

    private String hardwareNo;

    private String softwareNo;

    private String configWord;

    private String vehicleNodeCode;

    private Long bindingId;

    private String bindingState;

    private LocalDateTime bindingUpdatedAt;

    private List<TboxTimelineItemResult> activationMilestones;

    private TboxLinkKeysResult linkKeys;
}
