package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.DeviceAdmissionLogPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 设备接入鉴权审计日志表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-14
 */
@Mapper
public interface DeviceAdmissionLogMapper extends BaseDao<DeviceAdmissionLogPo, Long> {

}
