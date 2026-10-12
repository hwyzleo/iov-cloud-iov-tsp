package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * TBOX 运营查询条件
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class TboxManagementCriteria {

    private String sn;

    private String vin;

    private String partCode;

    private Integer deviceStatus;

    /**
     * 绑定状态（规范枚举名：UNBOUND/BOUND/BINDING_CONFLICT）
     */
    private String bindingStatus;

    /**
     * 异常类型（规范枚举名：TboxAnomalyType）
     */
    private String anomalyType;

    private LocalDateTime firstLoginFrom;

    private LocalDateTime firstLoginTo;

    private LocalDateTime lastLoginFrom;

    private LocalDateTime lastLoginTo;

    private LocalDateTime activateFrom;

    private LocalDateTime activateTo;

    private LocalDateTime createdFrom;

    private LocalDateTime createdTo;

    /**
     * 激活超时判定阈值 = now - activationTimeout
     */
    private LocalDateTime activationTimeoutBefore;

    /**
     * 长期未登录判定阈值 = now - longOfflineThreshold
     */
    private LocalDateTime longOfflineBefore;

    /**
     * 排序字段（白名单规范值）：lastLoginTime/firstLoginTime/activateTime/createdAt/updatedAt/deviceStatus
     */
    private String sortBy;

    /**
     * true=ASC，false=DESC
     */
    private boolean sortAsc;
}
