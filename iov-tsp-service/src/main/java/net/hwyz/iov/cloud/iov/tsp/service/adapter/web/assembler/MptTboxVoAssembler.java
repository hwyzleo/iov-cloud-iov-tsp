package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.framework.common.util.DisplayTimeUtil;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptTboxDetailVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptTboxExportVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptTboxSummaryVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptTboxTimelineVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.TboxAnomalyTagVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.TboxLinkKeysVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.TboxTimelineItemVo;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptTboxDetailResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptTboxSummaryResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptTboxTimelineResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.TboxAnomalyTagResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.TboxLinkKeysResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.TboxTimelineItemResult;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MPT TBOX 结果/VO 转换器
 * <p>
 * TSP-DSN-CR-015 RD-MPT-TZ-002/003：Application Result 保持 UTC 墙钟事实语义，
 * JSON 展示 VO 在此边界按有效展示时区（X-Timezone &gt; 默认 Asia/Shanghai）做一次投影转换；
 * Excel 导出从 UTC Result 直接分叉，禁止从已转换 JSON VO 二次转换。
 *
 * @author hwyz_leo
 */
public interface MptTboxVoAssembler {

    MptTboxVoAssembler INSTANCE = new MptTboxVoAssembler() {
    };

    // ---------- JSON 展示投影（按有效展示时区一次转换） ----------

    default MptTboxSummaryVo toVo(MptTboxSummaryResult result, ZoneId zone) {
        if (result == null) {
            return null;
        }
        return MptTboxSummaryVo.builder()
                .sn(result.getSn())
                .vin(result.getVin())
                .partCode(result.getPartCode())
                .deviceStatus(result.getDeviceStatus())
                .firstLoginTime(toDisplay(result.getFirstLoginTime(), zone))
                .lastLoginTime(toDisplay(result.getLastLoginTime(), zone))
                .activateTime(toDisplay(result.getActivateTime(), zone))
                .bindingStatus(result.getBindingStatus())
                .bindingVersion(result.getBindingVersion())
                .anomalyTags(toAnomalyTagVos(result.getAnomalyTags(), zone))
                .activationDurationSeconds(result.getActivationDurationSeconds())
                .lastLoginAgeSeconds(result.getLastLoginAgeSeconds())
                .maskedHsmUid(result.getMaskedHsmUid())
                .maskedImei(result.getMaskedImei())
                .maskedIccids(result.getMaskedIccids())
                .supplierCode(result.getSupplierCode())
                .hardwareVersion(result.getHardwareVersion())
                .softwareVersion(result.getSoftwareVersion())
                .createdAt(toDisplay(result.getCreatedAt(), zone))
                .updatedAt(toDisplay(result.getUpdatedAt(), zone))
                .build();
    }

    default MptTboxDetailVo toVo(MptTboxDetailResult result, ZoneId zone) {
        if (result == null) {
            return null;
        }
        return MptTboxDetailVo.builder()
                .summary(toVo(result.getSummary(), zone))
                .hardwareNo(result.getHardwareNo())
                .softwareNo(result.getSoftwareNo())
                .configWord(result.getConfigWord())
                .vehicleNodeCode(result.getVehicleNodeCode())
                .bindingId(result.getBindingId())
                .bindingState(result.getBindingState())
                .bindingUpdatedAt(toDisplay(result.getBindingUpdatedAt(), zone))
                .activationMilestones(toTimelineItemVos(result.getActivationMilestones(), zone))
                .linkKeys(toVo(result.getLinkKeys()))
                .build();
    }

    default MptTboxTimelineVo toVo(MptTboxTimelineResult result, ZoneId zone) {
        if (result == null) {
            return null;
        }
        return MptTboxTimelineVo.builder()
                .partial(result.isPartial())
                .milestones(toTimelineItemVos(result.getMilestones(), zone))
                .build();
    }

    default TboxAnomalyTagVo toVo(TboxAnomalyTagResult result, ZoneId zone) {
        if (result == null) {
            return null;
        }
        return TboxAnomalyTagVo.builder()
                .type(result.getType())
                .evaluatedAt(toDisplay(result.getEvaluatedAt(), zone))
                .since(toDisplay(result.getSince(), zone))
                .durationSeconds(result.getDurationSeconds())
                .build();
    }

    default TboxTimelineItemVo toVo(TboxTimelineItemResult result, ZoneId zone) {
        if (result == null) {
            return null;
        }
        return TboxTimelineItemVo.builder()
                .milestone(result.getMilestone())
                .occurredAt(toDisplay(result.getOccurredAt(), zone))
                .sourceRef(result.getSourceRef())
                .detail(result.getDetail())
                .build();
    }

    default TboxLinkKeysVo toVo(TboxLinkKeysResult result) {
        if (result == null) {
            return null;
        }
        return TboxLinkKeysVo.builder()
                .sn(result.getSn())
                .partCode(result.getPartCode())
                .vin(result.getVin())
                .bindingId(result.getBindingId())
                .build();
    }

    default List<TboxAnomalyTagVo> toAnomalyTagVos(List<TboxAnomalyTagResult> results, ZoneId zone) {
        if (results == null) {
            return null;
        }
        return results.stream().map(tag -> toVo(tag, zone)).toList();
    }

    default List<TboxTimelineItemVo> toTimelineItemVos(List<TboxTimelineItemResult> results, ZoneId zone) {
        if (results == null) {
            return null;
        }
        return results.stream().map(item -> toVo(item, zone)).toList();
    }

    // ---------- Excel 导出（UTC Result → 展示时区墙钟，防二次转换） ----------

    /**
     * 摘要结果转导出项（沿用查询脱敏结果，不提升敏感度）
     * <p>
     * TSP-DSN-CR-015 RD-MPT-TZ-005：从 UTC Result 直接生成，展示时区由
     * DisplayTimeFacade 按请求解析，不再读取 TSP 自有展示配置。
     *
     * @param result      摘要结果
     * @param displayZone 展示时区（UTC 墙钟 → 展示时区墙钟）
     * @return 导出项
     */
    default MptTboxExportVo toExportVo(MptTboxSummaryResult result, ZoneId displayZone) {
        if (result == null) {
            return null;
        }
        return MptTboxExportVo.builder()
                .sn(result.getSn())
                .vin(result.getVin())
                .partCode(result.getPartCode())
                .deviceStatus(result.getDeviceStatus() == null ? null : String.valueOf(result.getDeviceStatus()))
                .firstLoginTime(toDisplay(result.getFirstLoginTime(), displayZone))
                .lastLoginTime(toDisplay(result.getLastLoginTime(), displayZone))
                .activateTime(toDisplay(result.getActivateTime(), displayZone))
                .bindingStatus(result.getBindingStatus() == null ? null : result.getBindingStatus().name())
                .anomalyTags(result.getAnomalyTags() == null ? null : result.getAnomalyTags().stream()
                        .map(tag -> tag.getType().name())
                        .collect(Collectors.joining(",")))
                .activationDurationSeconds(result.getActivationDurationSeconds())
                .lastLoginAgeSeconds(result.getLastLoginAgeSeconds())
                .maskedHsmUid(result.getMaskedHsmUid())
                .maskedImei(result.getMaskedImei())
                .maskedIccids(result.getMaskedIccids() == null ? null : String.join(",", result.getMaskedIccids()))
                .supplierCode(result.getSupplierCode())
                .hardwareVersion(result.getHardwareVersion())
                .softwareVersion(result.getSoftwareVersion())
                .bindingVersion(result.getBindingVersion())
                .createdAt(toDisplay(result.getCreatedAt(), displayZone))
                .updatedAt(toDisplay(result.getUpdatedAt(), displayZone))
                .build();
    }

    /**
     * 摘要结果列表转导出项列表
     *
     * @param results     摘要结果列表
     * @param displayZone 展示时区
     * @return 导出项列表
     */
    default List<MptTboxExportVo> toExportVoList(List<MptTboxSummaryResult> results, ZoneId displayZone) {
        if (results == null) {
            return List.of();
        }
        return results.stream().map(result -> toExportVo(result, displayZone)).toList();
    }

    /**
     * UTC 墙钟（DB 存储语义）→ 展示时区墙钟。委托框架 COMMON DisplayTimeUtil，单一转换实现。
     *
     * @param utcWall     DB 读出的 UTC 墙钟 LocalDateTime
     * @param displayZone 展示时区
     * @return 展示时区墙钟 LocalDateTime；null 透传
     */
    default LocalDateTime toDisplay(LocalDateTime utcWall, ZoneId displayZone) {
        if (utcWall == null || displayZone == null) {
            return utcWall;
        }
        return DisplayTimeUtil.utcWallClockToLocal(utcWall, displayZone);
    }
}
