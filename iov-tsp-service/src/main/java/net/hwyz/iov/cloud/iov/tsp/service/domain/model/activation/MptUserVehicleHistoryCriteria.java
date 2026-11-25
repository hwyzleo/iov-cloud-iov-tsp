package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

/**
 * MPT 用户车辆关系历史查询条件（IOV-TSP-DSN-CR-021 D021-02）。
 * <p>
 * 条件在 SQL 中过滤，分页由数据库执行；空条件表示不限。
 *
 * @param vin        车辆 VIN
 * @param relationId 关系ID
 * @param operationId 解绑操作ID
 * @author hwyz_leo
 */
public record MptUserVehicleHistoryCriteria(String vin, String relationId, String operationId) {
}
