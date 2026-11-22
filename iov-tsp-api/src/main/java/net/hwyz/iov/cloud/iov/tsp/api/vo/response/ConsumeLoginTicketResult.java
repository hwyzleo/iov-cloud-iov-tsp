package net.hwyz.iov.cloud.iov.tsp.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 一次性登录票据消费结果（内部服务域，IOV-TSP-DSN-CR-019 D019-03）。
 * <p>
 * 成功消费后返回票据绑定的四要素与关系标识，供 IAM 签发用户令牌；
 * 本结果不包含任何令牌或长期凭据。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumeLoginTicketResult {

    /**
     * 认领账号（票据绑定要素）
     */
    private String accountId;

    /**
     * 车辆 VIN（票据绑定要素）
     */
    private String vin;

    /**
     * 来源会话ID（票据绑定要素）
     */
    private String sessionId;

    /**
     * 绑定操作ID（票据绑定要素）
     */
    private String bindingOperationId;

    /**
     * 绑定关系标识（令牌声明用，可为空）
     */
    private String relationId;

    /**
     * 票据到期绝对时刻（UTC）
     */
    private Instant expiresAt;
}
