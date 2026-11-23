package net.hwyz.iov.cloud.iov.tsp.api.vo.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.PrivacyAuthorizationState;

import java.util.List;

/**
 * 可信登录上下文补报隐私授权。
 * <p>
 * IOV-TSP-DSN-CR-017 D017-06：Account 取认证上下文、VIN 取可信车端上下文（X-Vin）；
 * 授权项按批准目录，关闭/拒绝不等于授权；首次弹框标志仍由车机维护。
 * <p>
 * IOV-TSP-DSN-CR-020 D020-03（G-1 定稿）：请求新增 {@code catalogVersion}，目录强校验生效
 * （{@code tsp.user-vehicle.privacy-authorization.enabled=true} 且 {@code enforce-catalog=true}）时
 * 必填且必须与服务端当前目录版本一致（否则 401119）；{@code authorizationItem} 必须命中当前目录
 * （否则 401118）；两者均整批拒绝、零写入。
 * <p>
 * 终审修复 B-1（兼容车端存量报文）：{@code catalogVersion} <b>不加 Bean Validation 断言</b>，
 * 改为 Service 内按配置条件必填 —— {@code enforce-catalog=false}（或目录能力关闭）时允许缺失，
 * 回退旧行为（不校验目录版本、不校验目录外授权项），仅日志/审计标记 {@code COMPAT_MODE}。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportPrivacyAuthorizationRequest {

    /**
     * 回执ID（幂等键）
     */
    @NotBlank(message = "回执ID不能为空")
    private String reportId;

    /**
     * 目录版本（<b>条件必填</b>：仅在目录强校验生效时必填）。
     * <p>
     * 目录强校验生效（{@code enabled=true} 且 {@code enforce-catalog=true}）时：必填且必须原样回传车端
     * 所拉取的目录版本，缺失 → 401112，与服务端当前目录不一致 → 401119；
     * {@code enforce-catalog=false}（回滚/兼容模式）时：允许缺失，回退旧行为（不校验目录版本、
     * 不校验目录外授权项），仅在日志与审计标记 {@code COMPAT_MODE}。
     * <p>
     * 说明：<b>不加</b> {@code @NotBlank}（终审修复 B-1）——Bean Validation 无法表达跨字段/按配置条件必填，
     * 若在此断言，则 {@code enforce-catalog=false} 也无法放行存量车端报文（100% 401112）。
     */
    private String catalogVersion;

    /**
     * 授权项状态列表
     */
    @Valid
    @NotEmpty(message = "授权项不能为空")
    private List<AuthorizationItem> items;

    /**
     * 用户动作时间（ISO-8601、带 Z/offset）
     */
    private String occurredAt;

    /**
     * 单个授权项上报
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorizationItem {

        /**
         * 授权项编码（批准目录内）
         */
        @NotBlank(message = "授权项不能为空")
        private String authorizationItem;

        /**
         * 授权状态
         */
        @jakarta.validation.constraints.NotNull(message = "授权状态不能为空")
        private PrivacyAuthorizationState state;

        /**
         * 客户端决策序号（IOV-TSP-DSN-CR-020 G-1 定稿：每次用户决策 +1 并由车端持久化）。
         * <p>
         * 仅用于同项单调收敛（大于已存 → 更新；等于 → 幂等；小于 → 忽略且不报错），
         * <b>不再与目录 currentVersion 比较</b>（目录不再承载每项版本号）。
         */
        @jakarta.validation.constraints.NotNull(message = "来源版本不能为空")
        @jakarta.validation.constraints.Min(value = 1, message = "来源版本必须大于等于 1")
        private Long version;
    }
}
