package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleIdcmPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆信息娱乐模块表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-23
 */
@Mapper
public interface VehicleIdcmMapper extends BaseDao<VehicleIdcmPo, Long> {

}
