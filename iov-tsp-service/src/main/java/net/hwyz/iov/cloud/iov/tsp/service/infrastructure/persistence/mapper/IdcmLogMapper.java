package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.IdcmLogPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 信息娱乐模块信息变更日志表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-15
 */
@Mapper
public interface IdcmLogMapper extends BaseDao<IdcmLogPo, Long> {

}
