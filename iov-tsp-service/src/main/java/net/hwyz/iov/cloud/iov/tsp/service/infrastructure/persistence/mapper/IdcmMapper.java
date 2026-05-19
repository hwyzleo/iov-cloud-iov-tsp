package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.IdcmPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 信息娱乐模块信息表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-15
 */
@Mapper
public interface IdcmMapper extends BaseDao<IdcmPo, Long> {

    /**
     * 根据序列号查询信息娱乐模块信息
     *
     * @param sn 序列号
     * @return 信息娱乐模块信息
     */
    IdcmPo selectBySn(String sn);

}
