package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingCallerScope;

import java.time.Instant;

/**
 * 车端会话创建幂等台账（IOV-TSP-DSN-CR-020 D020-04）。
 * <p>
 * 与会话解耦的独立台账（不给 {@code tb_activation_session} 加列）：
 * 唯一键 {@code (caller_scope, vin, idempotency_key)}，本期 {@code callerScope} 固定 VEHICLE；
 * {@code expiresAt} 与会话 expiresAt 对齐（TTL 到期即释放，不引入第二套 TTL 配置）。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationSessionIdempotency {

    private Long id;

    /**
     * 幂等记录业务ID
     */
    private String idempotencyId;

    /**
     * 调用方作用域（本期固定 VEHICLE，与 V17 绑定操作 caller_scope 风格对齐）
     */
    private BindingCallerScope callerScope;

    /**
     * 车辆 VIN（幂等作用域）
     */
    private String vin;

    /**
     * 调用方提供的幂等键
     */
    private String idempotencyKey;

    /**
     * 请求内容不可逆摘要（同键异体即冲突 401110）
     */
    private String requestDigest;

    /**
     * 复用/新建的会话业务ID
     */
    private String sessionId;

    /**
     * 幂等释放时刻（= 会话 expiresAt）
     */
    private Instant expiresAt;

    /**
     * 乐观版本
     */
    private Integer rowVersion;

    /**
     * 是否仍在幂等保留期内（服务端 Clock 判定）
     *
     * @param now 当前绝对时刻
     * @return 未释放返回 true
     */
    public boolean retainedAt(Instant now) {
        return expiresAt != null && now.isBefore(expiresAt);
    }
}
