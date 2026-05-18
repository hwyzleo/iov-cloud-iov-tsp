package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.CmdRecordPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * TBOX指令记录表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-08-30
 */
@Mapper
public interface CmdRecordMapper extends BaseDao<CmdRecordPo, Long> {

    /**
     * 根据指令ID查询指令记录
     *
     * @param cmdId 指令ID
     * @return 指令记录
     */
    CmdRecordPo selectPoByCmdId(String cmdId);

}
