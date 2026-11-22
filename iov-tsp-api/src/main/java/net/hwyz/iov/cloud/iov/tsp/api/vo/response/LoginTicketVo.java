package net.hwyz.iov.cloud.iov.tsp.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 一次性登录票据签发结果（IOV-TSP-DSN-CR-019 D019-02）。
 * <p>
 * 票据原值只在本响应出现一次，服务端仅保存不可逆摘要；不承载账号令牌、
 * 账号明文或协议正文。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginTicketVo {

    /**
     * 票据原值（base64url 无填充，仅本次响应返回）
     */
    private String ticket;

    /**
     * 票据到期绝对时刻（UTC）
     */
    private Instant expiresAt;

    /**
     * 来源会话ID
     */
    private String sessionId;

    /**
     * 绑定关系标识（供 IAM 令牌声明，可为空）
     */
    private String relationId;
}
