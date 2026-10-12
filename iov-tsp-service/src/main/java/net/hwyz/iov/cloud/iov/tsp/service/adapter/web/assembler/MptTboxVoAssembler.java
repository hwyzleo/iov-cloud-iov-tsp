package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

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
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MPT TBOX 结果/VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptTboxVoAssembler {

    MptTboxVoAssembler INSTANCE = Mappers.getMapper(MptTboxVoAssembler.class);

    MptTboxSummaryVo toVo(MptTboxSummaryResult result);

    MptTboxDetailVo toVo(MptTboxDetailResult result);

    MptTboxTimelineVo toVo(MptTboxTimelineResult result);

    TboxAnomalyTagVo toVo(TboxAnomalyTagResult result);

    TboxTimelineItemVo toVo(TboxTimelineItemResult result);

    TboxLinkKeysVo toVo(TboxLinkKeysResult result);

    /**
     * 摘要结果转导出项（沿用查询脱敏结果，不提升敏感度）
     *
     * @param result 摘要结果
     * @return 导出项
     */
    default MptTboxExportVo toExportVo(MptTboxSummaryResult result) {
        if (result == null) {
            return null;
        }
        return MptTboxExportVo.builder()
                .sn(result.getSn())
                .vin(result.getVin())
                .partCode(result.getPartCode())
                .deviceStatus(result.getDeviceStatus() == null ? null : String.valueOf(result.getDeviceStatus()))
                .firstLoginTime(result.getFirstLoginTime())
                .lastLoginTime(result.getLastLoginTime())
                .activateTime(result.getActivateTime())
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
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .build();
    }

    /**
     * 摘要结果列表转导出项列表
     *
     * @param results 摘要结果列表
     * @return 导出项列表
     */
    default List<MptTboxExportVo> toExportVoList(List<MptTboxSummaryResult> results) {
        if (results == null) {
            return List.of();
        }
        return results.stream().map(this::toExportVo).toList();
    }
}
