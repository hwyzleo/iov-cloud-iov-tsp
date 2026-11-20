package net.hwyz.iov.cloud.iov.tsp.api.vo.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 车端申请激活二维码会话。
 * <p>
 * IOV-TSP-DSN-CR-017 D017-02：可信车端身份由网关上下文提供（X-Client-Type/X-Vin），
 * 请求体不接受自报 VIN；必须携带两项协议的明确同意证据。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateActivationSessionRequest {

    /**
     * 两项协议（隐私政策、用户协议）的展示修订与明确同意证据
     */
    @Valid
    @NotEmpty(message = "协议同意证据不能为空")
    private List<AgreementAcceptanceRequest> agreements;
}
