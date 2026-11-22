package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

import java.time.LocalDateTime;

/**
 * 一次性登录票据数据对象（IOV-TSP-DSN-CR-019）。
 * <p>
 * 只保存票据摘要；时间列按 UTC 绝对时刻存储（DATETIME(3)），转换在仓储实现显式完成。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationLoginTicketPo extends BasePo {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String ticketId;

    private String ticketHash;

    private String sessionId;

    private String bindingOperationId;

    private String vin;

    private String accountId;

    private String relationId;

    private String state;

    private LocalDateTime issuedAt;

    private LocalDateTime expiresAt;

    private LocalDateTime consumedAt;

    private String consumedBy;

    private Integer issueSeq;
}
