package net.hwyz.iov.cloud.iov.tsp.api.vo.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 车端长按跳过激活。
 * <p>
 * IOV-TSP-DSN-CR-017 D017-02/D017-06：仅幂等记录 VIN 级协议同意与跳过结果，
 * 不创建用户关系、不产生登录成功与首次激活事实。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkipActivationRequest {

    /**
     * 本次跳过的来源回执ID（幂等键）
     */
    @NotBlank(message = "回执ID不能为空")
    private String reportId;

    /**
     * 车端会话ID（可选，用于追溯）
     */
    private String sourceSessionId;

    /**
     * 两项协议的展示修订与明确同意证据
     */
    @Valid
    @NotEmpty(message = "协议同意证据不能为空")
    private List<AgreementAcceptanceRequest> agreements;

    /**
     * 用户动作时间（ISO-8601、带 Z/offset）；缺失表示未提供，不用接收时间冒充
     */
    private String occurredAt;
}
