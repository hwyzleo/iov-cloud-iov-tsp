package net.hwyz.iov.cloud.iov.tsp.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxAnomalyType;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxBindingStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MPT TBOX 列表摘要结果
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptTboxSummaryResult {

    private String sn;

    private String vin;

    private String partCode;

    private Integer deviceStatus;

    private LocalDateTime firstLoginTime;

    private LocalDateTime lastLoginTime;

    private LocalDateTime activateTime;

    private TboxBindingStatus bindingStatus;

    private Long bindingVersion;

    private List<TboxAnomalyTagResult> anomalyTags;

    private Long activationDurationSeconds;

    private Long lastLoginAgeSeconds;

    private String maskedHsmUid;

    private String maskedImei;

    private List<String> maskedIccids;

    private String supplierCode;

    private String hardwareVersion;

    private String softwareVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 主异常标签（优先级最高），便于导出展示
     */
    private TboxAnomalyType primaryAnomalyType;
}
