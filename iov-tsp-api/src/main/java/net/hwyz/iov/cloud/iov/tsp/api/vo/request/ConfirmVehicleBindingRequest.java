package net.hwyz.iov.cloud.iov.tsp.api.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户确认绑定请求。
 * <p>
 * IOV-TSP-DSN-CR-017 D017-04：只接收会话与幂等键；账号取可信认证上下文，账号/VIN 不得由请求体作为权威。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmVehicleBindingRequest {

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /**
     * 幂等键（调用方生成，同键同内容返回原结果）
     */
    @NotBlank(message = "幂等键不能为空")
    @Size(max = 128, message = "幂等键长度不能超过 128")
    private String idempotencyKey;
}
