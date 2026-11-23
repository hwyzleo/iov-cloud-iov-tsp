package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

import java.time.LocalDateTime;

/**
 * 车端会话创建幂等台账数据对象（IOV-TSP-DSN-CR-020 V19）。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationSessionIdempotencyPo extends BasePo {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String idempotencyId;

    private String callerScope;

    private String vin;

    private String idempotencyKey;

    private String requestDigest;

    private String sessionId;

    /**
     * 幂等释放时刻（UTC 墙钟）
     */
    private LocalDateTime expiresAt;

    private Integer rowVersion;
}
