package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleNetworkLogPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆网联信息变更日志表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-19
 */
@Mapper
public interface VehicleNetworkLogDao extends BaseDao<VehicleNetworkLogPo, Long> {

}
