package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * <p>
 * 车联终端信息表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-14
 */
@Mapper
public interface TboxMapper extends BaseDao<TboxPo, Long> {

    /**
     * 根据序列号查询车联终端信息
     *
     * @param sn 序列号
     * @return 车联终端信息
     */
    TboxPo selectBySn(String sn);

    /**
     * 根据HSM查询车联终端信息
     *
     * @param hsm 硬件安全模块标识
     * @return 车联终端信息
     */
    TboxPo selectByHsm(String hsm);

    /**
     * 激活设备（条件更新）
     * 仅当 device_status=PRE_ACTIVE(1) 且 activate_time IS NULL 时更新
     *
     * @param sn 设备序列号
     * @param activateTime 激活时间
     * @param activeStatus 激活后的状态值
     * @return 更新行数
     */
    int activateDevice(@Param("sn") String sn, @Param("activateTime") LocalDateTime activateTime, @Param("activeStatus") Integer activeStatus);

}
