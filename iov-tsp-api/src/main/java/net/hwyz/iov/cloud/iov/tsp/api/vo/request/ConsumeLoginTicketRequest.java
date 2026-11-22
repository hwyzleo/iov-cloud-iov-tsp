package net.hwyz.iov.cloud.iov.tsp.api.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一次性登录票据消费请求（内部服务域，IOV-TSP-DSN-CR-019 D019-03）。
 * <p>
 * 仅携带票据原值；车辆、会话、绑定操作与账号事实一律以服务端票据记录为准，
 * 不接受调用方自报绑定事实。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumeLoginTicketRequest {

    /**
     * 票据原值（base64url 无填充）
     */
    @NotBlank(message = "登录票据不能为空")
    private String ticket;
}
