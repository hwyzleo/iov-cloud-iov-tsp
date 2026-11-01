package net.hwyz.iov.cloud.iov.tsp.api.vo.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MPT TBOX 列表/导出查询请求
 * <p>
 * TSP-DSN-CR-015：时间筛选参数保留原始 ISO 文本（展示时区墙钟，无偏移），
 * 由 MPT 查询端点按有效展示时区严格解析后转换为 UTC 墙钟查询值；
 * 不再由 Spring 直接绑定为 LocalDateTime，避免丢失文本与隐式时区猜测。
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

    /**
     * 首次登录时间范围（展示时区墙钟 ISO 文本，如 2026-11-01T15:00:00；可带显式 offset 消歧）
     */
    private String firstLoginFrom;

    private String firstLoginTo;

    private String lastLoginFrom;

    private String lastLoginTo;

    private String activateFrom;

    private String activateTo;

    private String createdFrom;

    private String createdTo;

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
