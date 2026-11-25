package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.MptUserVehicleHistoryCriteria;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.MptUserVehicleHistoryItem;

import java.util.List;

/**
 * MPT 用户车辆关系专用只读查询仓储（IOV-TSP-DSN-CR-021 D021-02）。
 * <p>
 * 过滤与分页由数据库完成；本接口不提供内存过滤能力。
 *
 * @author hwyz_leo
 */
public interface MptUserVehicleQueryRepository {

    /**
     * 按条件查询关系历史（物理分页由调用方通过 PageHelper 上下文驱动）
     *
     * @param criteria 查询条件
     * @return 当前页历史行
     */
    List<MptUserVehicleHistoryItem> searchHistory(MptUserVehicleHistoryCriteria criteria);
}
