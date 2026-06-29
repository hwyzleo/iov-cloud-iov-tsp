package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleSimLogPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆SIM卡日志表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-06-29
 */
@Mapper
public interface VehicleSimLogMapper extends BaseDao<VehicleSimLogPo, Long> {

}
