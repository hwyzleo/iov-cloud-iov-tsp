package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

import java.time.LocalDateTime;

/**
 * 首次激活候选事实与投递 Outbox 数据对象。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationFactPo extends BasePo {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String eventId;

    private Integer schemaVersion;

    private String vin;

    private String bindingOperationId;

    private String sourceFactRef;

    private LocalDateTime occurredAt;

    private String payloadDigest;

    private String publishStatus;

    private Integer retryCount;

    private String lastError;

    private LocalDateTime publishedAt;
}
