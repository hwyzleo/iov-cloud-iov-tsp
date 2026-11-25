package net.hwyz.iov.cloud.iov.tsp.api.vo.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MPT 用户车辆关系历史查询请求（IOV-TSP-DSN-CR-021 D021-02）。
 * <p>
 * 过滤与分页全部在数据库侧完成（禁止内存分页）；所有过滤条件均可选，
 * pageNo/pageSize 由 MPT 查询适配层规整到安全范围。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptUserVehicleHistoryRequest {

    /**
     * 车辆 VIN（可选，精确匹配）
     */
    private String vin;

    /**
     * 关系ID（可选，精确匹配）
     */
    private String relationId;

    /**
     * 解绑操作ID（可选，精确匹配）
     */
    private String operationId;

    private Integer pageNo;

    private Integer pageSize;
}
