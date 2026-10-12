package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxManagementCriteria;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxManagementView;

import java.util.List;

/**
 * TBOX 后台运营查询仓储接口（只读）
 *
 * @author hwyz_leo
 */
public interface TboxManagementQueryRepository {

    /**
     * 按条件查询 TBOX 聚合视图（分页由调用方通过 PageHelper 控制）
     *
     * @param criteria 查询条件
     * @return 聚合视图列表
     */
    List<TboxManagementView> search(TboxManagementCriteria criteria);

    /**
     * 按 SN 查询单个 TBOX 聚合视图
     *
     * @param sn 设备序列号
     * @return 聚合视图，不存在返回 null
     */
    TboxManagementView getBySn(String sn);
}
