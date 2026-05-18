package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.SimPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * SIM卡信息表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-12
 */
@Mapper
public interface SimDao extends BaseDao<SimPo, Long> {

    /**
     * 根据ICCID查询SIM卡信息
     *
     * @param iccid ICCID
     * @return SIM卡信息
     */
    SimPo selectByIccid(String iccid);

}
