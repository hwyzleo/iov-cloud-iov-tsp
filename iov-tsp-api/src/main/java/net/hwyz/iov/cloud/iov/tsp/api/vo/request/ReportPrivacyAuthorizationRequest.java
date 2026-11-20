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
         * 来源版本（按来源顺序收敛，不用接收时间覆盖）
         */
        @jakarta.validation.constraints.NotNull(message = "来源版本不能为空")
        private Long version;
    }
}
