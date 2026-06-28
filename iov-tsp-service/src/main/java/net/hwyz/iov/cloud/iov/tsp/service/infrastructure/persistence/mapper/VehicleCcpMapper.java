package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleCcpPo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 车辆中央计算平台表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-23
 */
@Mapper
public interface VehicleCcpMapper extends BaseDao<VehicleCcpPo, Long> {

    /**
     * 根据 binding_id 幂等 upsert
     */
    int upsertByBindingId(VehicleCcpPo po);

    /**
     * 根据 binding_id 查询
     */
    VehicleCcpPo selectByBindingId(Long bindingId);

    /**
     * 获取所有活跃绑定
     */
    List<VehicleCcpPo> selectAllActiveBindings();
}
