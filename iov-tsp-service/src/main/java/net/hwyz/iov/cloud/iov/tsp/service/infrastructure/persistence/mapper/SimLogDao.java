package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.SimLogPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * SIM卡信息变更日志表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-13
 */
@Mapper
public interface SimLogDao extends BaseDao<SimLogPo, Long> {

    /**
     * 根据ICCID批量删除
     *
     * @param iccid ICCID
     * @return 删除结果
     */
    int physicalDeletePoByIccid(String iccid);

}
