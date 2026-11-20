package net.hwyz.iov.cloud.iov.tsp.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 二维码会话签发结果。
 * <p>
 * 二维码只承载高熵一次性凭据，不含 PII、长期令牌或可直接登录的授权材料。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateActivationSessionVo {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 二维码凭据（仅本次响应返回，服务端只存摘要）
     */
    private String qrToken;

    /**
     * 会话到期绝对时刻
     */
    private Instant expiresAt;
}
