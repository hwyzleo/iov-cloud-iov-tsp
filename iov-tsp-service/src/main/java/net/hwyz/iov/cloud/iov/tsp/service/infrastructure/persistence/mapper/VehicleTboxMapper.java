package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleTboxPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 车辆车联终端表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-23
 */
@Mapper
public interface VehicleTboxMapper extends BaseDao<VehicleTboxPo, Long> {

    /**
     * 根据 binding_id 幂等 upsert
     */
    int upsertByBindingId(VehicleTboxPo po);

    /**
     * 根据 binding_id 查询
     */
    VehicleTboxPo selectByBindingId(Long bindingId);

    /**
     * 获取所有活跃绑定
     */
    List<VehicleTboxPo> selectAllActiveBindings();

    /**
     * 根据SN和绑定状态查询
     */
    VehicleTboxPo selectBySnAndBindState(@Param("sn") String sn, @Param("bindState") String bindState);
}
