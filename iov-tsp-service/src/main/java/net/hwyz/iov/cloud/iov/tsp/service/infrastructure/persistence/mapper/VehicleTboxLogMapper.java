package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleTboxLogPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆车联终端变更日志表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-23
 */
@Mapper
public interface VehicleTboxLogMapper extends BaseDao<VehicleTboxLogPo, Long> {

}
