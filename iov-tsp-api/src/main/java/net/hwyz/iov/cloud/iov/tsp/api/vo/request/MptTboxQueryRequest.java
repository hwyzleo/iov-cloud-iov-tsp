package net.hwyz.iov.cloud.iov.tsp.api.vo.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * MPT TBOX 列表/导出查询请求
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptTboxQueryRequest {

    private String sn;

    private String vin;

    private String partCode;

    private Integer deviceStatus;

    /**
     * 绑定状态：UNBOUND/BOUND/BINDING_CONFLICT
     */
    private String bindingStatus;

    /**
     * 异常类型：TboxAnomalyType
     */
    private String anomalyType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime firstLoginFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime firstLoginTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime lastLoginFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime lastLoginTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime activateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime activateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdTo;

    /**
     * 排序字段白名单：lastLoginTime/firstLoginTime/activateTime/createdAt/updatedAt/deviceStatus
     */
    private String sortBy;

    /**
     * 排序方向：asc/desc
     */
    private String sortDirection;

    private Integer pageNo;

    private Integer pageSize;
}
