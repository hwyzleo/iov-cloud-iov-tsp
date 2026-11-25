package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.framework.common.util.DisplayTimeUtil;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptUnbindOperationVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptUserVehicleHistoryVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptUserVehicleRelationVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptUserVehicleUnbindVo;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUnbindOperationResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUserVehicleHistoryResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUserVehicleRelationResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUserVehicleUnbindResult;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.SensitiveDataMasker;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * MPT 用户车辆关系 Result/VO 转换（IOV-TSP-DSN-CR-021 D021-02/D021-10）。
 * <p>
 * 展示时区只在 Adapter/VO 边界应用一次（CR-015）：详情/历史/操作查询字段输出无偏移墙钟；
 * 解绑/人工重试写响应保留绝对 UTC 时刻（带 Z），二者不混用。
 * 账号一律脱敏，不输出完整账号、票据或 token。
 *
 * @author hwyz_leo
 */
public interface MptUserVehicleVoAssembler {

    MptUserVehicleVoAssembler INSTANCE = new MptUserVehicleVoAssembler() {
    };

    /**
     * 详情展示投影（展示时区墙钟）
     *
     * @param result 查询结果（UTC 事实）
     * @param zone   生效展示时区
     * @return 展示 VO
     */
    default MptUserVehicleRelationVo toVo(MptUserVehicleRelationResult result, ZoneId zone) {
        if (result == null) {
            return null;
        }
        return MptUserVehicleRelationVo.builder()
                .vin(result.getVin())
                .bindingStatus(result.getBindingStatus())
                .detailStatus(result.getDetailStatus())
                .relation(toRelationVo(result.getRelation(), zone))
                .latestReleasedRelation(toLatestReleasedVo(result.getLatestReleasedRelation(), zone))
                .iviLogin(toIviLoginVo(result.getIviLogin(), zone))
                .firstActivation(toFirstActivationVo(result.getFirstActivation(), zone))
                .latestUnbind(toUnbindInfoVo(result.getLatestUnbind(), zone))
                .build();
    }

    /**
     * 解绑/重试写响应（绝对 UTC）
     *
     * @param result 写结果
     * @return 响应 VO
     */
    default MptUserVehicleUnbindVo toUnbindVo(MptUserVehicleUnbindResult result) {
        if (result == null) {
            return null;
        }
        return MptUserVehicleUnbindVo.builder()
                .operationId(result.getOperationId())
                .relationId(result.getRelationId())
                .vin(result.getVin())
                .relationState(result.getRelationState())
                .credentialRevocationStatus(result.getCredentialRevocationStatus())
                .releasedAt(result.getReleasedAt())
                .revokedSessionCount(result.getRevokedSessionCount())
                .build();
    }

    /**
     * 解绑操作查询投影（展示时区墙钟）
     *
     * @param result 查询结果（UTC 事实）
     * @param zone   生效展示时区
     * @return 查询 VO
     */
    default MptUnbindOperationVo toOperationVo(MptUnbindOperationResult result, ZoneId zone) {
        if (result == null) {
            return null;
        }
        return MptUnbindOperationVo.builder()
                .operationId(result.getOperationId())
                .relationId(result.getRelationId())
                .vin(result.getVin())
                .relationState(result.getRelationState())
                .credentialRevocationStatus(result.getCredentialRevocationStatus())
                .revokedSessionCount(result.getRevokedSessionCount())
                .retryCount(result.getRetryCount())
                .releasedAt(toDisplay(result.getReleasedAt(), zone))
                .completedAt(toDisplay(result.getCompletedAt(), zone))
                .nextRetryAt(toDisplay(result.getNextRetryAt(), zone))
                .lastError(result.getLastError())
                .build();
    }

    /**
     * 历史分页项展示投影（展示时区墙钟 + 账号脱敏）
     *
     * @param result 查询结果（UTC 事实）
     * @param zone   生效展示时区
     * @return 历史 VO
     */
    default MptUserVehicleHistoryVo toHistoryVo(MptUserVehicleHistoryResult result, ZoneId zone) {
        if (result == null) {
            return null;
        }
        return MptUserVehicleHistoryVo.builder()
                .relationId(result.getRelationId())
                .vin(result.getVin())
                .maskedAccountId(maskAccount(result.getAccountId()))
                .role(result.getRole())
                .state(result.getState())
                .boundAt(toDisplay(result.getBoundAt(), zone))
                .releasedAt(toDisplay(result.getReleasedAt(), zone))
                .releaseReason(result.getReleaseReason())
                .releaseOperationId(result.getReleaseOperationId())
                .releasedBy(result.getReleasedBy())
                .credentialRevocationStatus(result.getCredentialRevocationStatus())
                .build();
    }

    /**
     * 账号脱敏（前 3 后 2），不输出完整账号
     *
     * @param accountId 原始账号
     * @return 脱敏账号
     */
    default String maskAccount(String accountId) {
        return SensitiveDataMasker.mask(accountId, 3, 2);
    }

    // ================= 内部投影 =================

    default MptUserVehicleRelationVo.Relation toRelationVo(MptUserVehicleRelationResult.RelationResult result,
                                                           ZoneId zone) {
        if (result == null) {
            return null;
        }
        return MptUserVehicleRelationVo.Relation.builder()
                .relationId(result.getRelationId())
                .maskedAccountId(maskAccount(result.getAccountId()))
                .role(result.getRole())
                .state(result.getState())
                .boundAt(toDisplay(result.getBoundAt(), zone))
                .releasedAt(toDisplay(result.getReleasedAt(), zone))
                .releaseReason(result.getReleaseReason())
                .build();
    }

    default MptUserVehicleRelationVo.LatestReleasedRelation toLatestReleasedVo(
            MptUserVehicleRelationResult.LatestReleasedRelationResult result, ZoneId zone) {
        if (result == null) {
            return null;
        }
        return MptUserVehicleRelationVo.LatestReleasedRelation.builder()
                .relationId(result.getRelationId())
                .maskedAccountId(maskAccount(result.getAccountId()))
                .boundAt(toDisplay(result.getBoundAt(), zone))
                .releasedAt(toDisplay(result.getReleasedAt(), zone))
                .releaseReason(result.getReleaseReason())
                .build();
    }

    default MptUserVehicleRelationVo.IviLogin toIviLoginVo(MptUserVehicleRelationResult.IviLoginResult result,
                                                           ZoneId zone) {
        if (result == null) {
            return null;
        }
        return MptUserVehicleRelationVo.IviLogin.builder()
                .state(result.getState())
                .reportedAt(toDisplay(result.getReportedAt(), zone))
                .build();
    }

    default MptUserVehicleRelationVo.FirstActivation toFirstActivationVo(
            MptUserVehicleRelationResult.FirstActivationResult result, ZoneId zone) {
        if (result == null) {
            return null;
        }
        return MptUserVehicleRelationVo.FirstActivation.builder()
                .state(result.getState())
                .firstActivatedAt(toDisplay(result.getFirstActivatedAt(), zone))
                .build();
    }

    default MptUserVehicleRelationVo.UnbindInfo toUnbindInfoVo(
            MptUserVehicleRelationResult.UnbindInfoResult result, ZoneId zone) {
        if (result == null) {
            return null;
        }
        return MptUserVehicleRelationVo.UnbindInfo.builder()
                .operationId(result.getOperationId())
                .releasedAt(toDisplay(result.getReleasedAt(), zone))
                .releaseReason(result.getReleaseReason())
                .credentialRevocationStatus(result.getCredentialRevocationStatus())
                .revokedSessionCount(result.getRevokedSessionCount())
                .build();
    }

    default LocalDateTime toDisplay(Instant value, ZoneId zone) {
        return value == null ? null : DisplayTimeUtil.toLocalDateTime(value, zone);
    }
}
