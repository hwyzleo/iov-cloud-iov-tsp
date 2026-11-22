package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.LoginTicketState;

import java.time.Instant;

/**
 * 一次性登录票据（IOV-TSP-DSN-CR-019 D019-01）。
 * <p>
 * 票据只是「绑定事实 → 登录授权」的搬运凭证：不落明文（仅不可逆摘要）、
 * 短 TTL、单次消费、绑定会话/绑定操作/车辆/认领账号四要素。
 * 到期判定使用服务端 Clock，不依赖后台清理任务。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationLoginTicket {

    private Long id;

    /**
     * 票据业务标识（对外引用，非凭据本体）
     */
    private String ticketId;

    /**
     * 票据原值的不可逆摘要（SHA-256 十六进制）
     */
    private String ticketHash;

    /**
     * 来源会话ID
     */
    private String sessionId;

    /**
     * 绑定操作ID
     */
    private String bindingOperationId;

    /**
     * 可信车辆 VIN
     */
    private String vin;

    /**
     * 认领账号
     */
    private String accountId;

    /**
     * 绑定关系标识（消费后返回，供令牌声明）
     */
    private String relationId;

    /**
     * 票据状态
     */
    private LoginTicketState state;

    /**
     * 签发绝对时刻
     */
    private Instant issuedAt;

    /**
     * 到期绝对时刻
     */
    private Instant expiresAt;

    /**
     * 消费绝对时刻
     */
    private Instant consumedAt;

    /**
     * 消费方标识
     */
    private String consumedBy;

    /**
     * 同一会话签发序号（重签计数）
     */
    private Integer issueSeq;

    /**
     * 乐观版本
     */
    private Integer rowVersion;

    /**
     * 是否已到期（服务端 Clock 判定）
     *
     * @param now 当前绝对时刻
     * @return 已到期返回 true
     */
    public boolean isExpired(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }
}
