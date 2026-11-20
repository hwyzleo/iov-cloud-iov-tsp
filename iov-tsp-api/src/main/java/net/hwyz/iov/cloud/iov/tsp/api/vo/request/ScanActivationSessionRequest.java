package net.hwyz.iov.cloud.iov.tsp.api.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * App 扫码请求。
 * <p>
 * 扫码不等于绑定成功；仅认领会话并返回最小化待确认车辆信息。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanActivationSessionRequest {

    /**
     * 二维码凭据（高熵随机串；服务端只存摘要）
     */
    @NotBlank(message = "二维码凭据不能为空")
    private String qrToken;
}
