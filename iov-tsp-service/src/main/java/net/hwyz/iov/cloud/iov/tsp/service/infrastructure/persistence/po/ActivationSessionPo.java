package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

import java.time.LocalDateTime;

/**
 * 一次性激活会话数据对象（IOV-TSP-DSN-CR-017）。
 * <p>
 * 时间列按 UTC 绝对时刻存储（DATETIME(3)），转换在仓储实现显式完成。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationSessionPo extends BasePo {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String sessionId;

    private String qrTokenHash;

    private String trustedVehicleRef;

    private String vin;

    private String agreementRevisions;

    /**
     * 到期时刻（UTC 墙钟）
     */
    private LocalDateTime expiresAt;

    private String state;

    private String claimedAccountId;

    private LocalDateTime claimedAt;

    private String bindingOperationId;

    private LocalDateTime skippedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime boundAt;

    private LocalDateTime resultExpiresAt;

    private String iviLoginState;

    private String iviLoginReportId;

    private LocalDateTime iviLoginReportedAt;
}
