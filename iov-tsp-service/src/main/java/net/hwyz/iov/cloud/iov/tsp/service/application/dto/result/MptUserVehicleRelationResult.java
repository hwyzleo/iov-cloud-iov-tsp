package net.hwyz.iov.cloud.iov.tsp.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CredentialRevocationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.FirstActivationState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.IviLoginResultStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.MptUserVehicleDetailStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UserVehicleBindingStatus;

import java.time.Instant;

/**
 * MPT 车辆详情用户绑定/激活查询结果（IOV-TSP-DSN-CR-021 D021-02）。
 * <p>
 * 三个事实区独立表达；时间保持 UTC 绝对事实，展示投影只在 Adapter/VO 边界完成。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptUserVehicleRelationResult {

    private String vin;

    private UserVehicleBindingStatus bindingStatus;

    private MptUserVehicleDetailStatus detailStatus;

    private RelationResult relation;

    private LatestReleasedRelationResult latestReleasedRelation;

    private IviLoginResult iviLogin;

    private FirstActivationResult firstActivation;

    private UnbindInfoResult latestUnbind;

    /**
     * 当前 ACTIVE 关系
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationResult {

        private String relationId;

        private String accountId;

        private String role;

        private String state;

        private Instant boundAt;

        private Instant releasedAt;

        private String releaseReason;
    }

    /**
     * 最近一次已释放关系（历史信息）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LatestReleasedRelationResult {

        private String relationId;

        private String accountId;

        private Instant boundAt;

        private Instant releasedAt;

        private String releaseReason;
    }

    /**
     * 车机登录事实
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IviLoginResult {

        private IviLoginResultStatus state;

        private Instant reportedAt;
    }

    /**
     * 首次激活事实
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FirstActivationResult {

        private FirstActivationState state;

        private Instant firstActivatedAt;
    }

    /**
     * 最近解绑操作摘要
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnbindInfoResult {

        private String operationId;

        private Instant releasedAt;

        private String releaseReason;

        private CredentialRevocationStatus credentialRevocationStatus;

        private Integer revokedSessionCount;
    }
}
