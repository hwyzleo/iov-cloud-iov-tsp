package net.hwyz.iov.cloud.iov.tsp.api.vo.mpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CredentialRevocationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.FirstActivationState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.IviLoginResultStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.MptUserVehicleDetailStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UserVehicleBindingStatus;

import java.time.LocalDateTime;

/**
 * MPT 车辆详情「用户绑定/激活」数据区（IOV-TSP-DSN-CR-021 D021-02）。
 * <p>
 * 三个事实区（用户绑定、车机登录、首次激活）独立表达，任一状态不得推导另一状态。
 * 时间字段为生效展示时区的无偏移墙钟（CR-015），事实源头仍为 UTC。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptUserVehicleRelationVo {

    /**
     * 车辆 VIN
     */
    private String vin;

    /**
     * 当前绑定状态 BOUND/UNBOUND（UNBOUND 时 relation 仅可能为最近已释放历史）
     */
    private UserVehicleBindingStatus bindingStatus;

    /**
     * 详情数据完整度：外部只读依赖不可用时为 PARTIAL
     */
    private MptUserVehicleDetailStatus detailStatus;

    /**
     * 当前 ACTIVE 关系；无 ACTIVE 时为 null
     */
    private Relation relation;

    /**
     * 最近一次已释放关系（仅 UNBOUND 时可能返回，作为历史信息）
     */
    private LatestReleasedRelation latestReleasedRelation;

    /**
     * 车机登录事实（独立于绑定状态）
     */
    private IviLogin iviLogin;

    /**
     * 整车首次激活事实（来自 VMD 只读契约）
     */
    private FirstActivation firstActivation;

    /**
     * 最近一次解绑操作（含撤销状态）
     */
    private UnbindInfo latestUnbind;

    /**
     * 当前或历史关系公开运营信息（账号脱敏）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Relation {

        private String relationId;

        /**
         * 脱敏账号标识（不返回完整账号）
         */
        private String maskedAccountId;

        private String role;

        /**
         * ACTIVE/RELEASED
         */
        private String state;

        private LocalDateTime boundAt;

        private LocalDateTime releasedAt;

        private String releaseReason;

        /**
         * 当前 ACTIVE 关系乐观版本；仅用于管理后台解绑 CAS，不是账号或授权事实。
         */
        private Integer rowVersion;
    }

    /**
     * 最近一次已释放关系（历史信息，不代表当前授权）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LatestReleasedRelation {

        private String relationId;

        private String maskedAccountId;

        private LocalDateTime boundAt;

        private LocalDateTime releasedAt;

        private String releaseReason;
    }

    /**
     * 车机登录事实（来自关系来源绑定操作对应会话）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IviLogin {

        private IviLoginResultStatus state;

        private LocalDateTime reportedAt;
    }

    /**
     * 首次激活事实（VMD 只读）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FirstActivation {

        private FirstActivationState state;

        private LocalDateTime firstActivatedAt;
    }

    /**
     * 最近一次解绑操作摘要
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnbindInfo {

        private String operationId;

        private LocalDateTime releasedAt;

        private String releaseReason;

        private CredentialRevocationStatus credentialRevocationStatus;

        private Integer revokedSessionCount;
    }
}
