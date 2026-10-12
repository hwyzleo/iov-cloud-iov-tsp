package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxManagementCriteria;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxManagementView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * TBOX 后台运营聚合查询 Mapper（只读）
 * <p>
 * TSP-DSN-CR-009：tb_tbox 与 tb_vehicle_tbox（TBOX grain 聚合）组合查询，禁止逐行调用外部服务。
 *
 * @author hwyz_leo
 */
@Mapper
public interface TboxManagementQueryMapper {

    /**
     * 按条件查询聚合视图（分页由 PageHelper 控制）
     *
     * @param criteria 查询条件
     * @return 聚合视图列表
     */
    List<TboxManagementView> selectPageByCriteria(TboxManagementCriteria criteria);

    /**
     * 按 SN 查询单个聚合视图
     *
     * @param sn 设备序列号
     * @return 聚合视图
     */
    TboxManagementView selectBySn(@Param("sn") String sn);
}
