package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.CcpLogPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 中央计算平台信息变更日志表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-14
 */
@Mapper
public interface CcpLogMapper extends BaseDao<CcpLogPo, Long> {

}
