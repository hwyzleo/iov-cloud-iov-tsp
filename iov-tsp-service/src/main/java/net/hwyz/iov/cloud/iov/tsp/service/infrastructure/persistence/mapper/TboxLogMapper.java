package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxLogPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车联终端信息变更日志表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-14
 */
@Mapper
public interface TboxLogMapper extends BaseDao<TboxLogPo, Long> {

}
