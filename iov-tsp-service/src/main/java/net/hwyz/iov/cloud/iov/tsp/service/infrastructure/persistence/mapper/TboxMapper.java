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
     * 记录成功登录（幂等）
     * first_login_time set-once；last_login_time 取 max(existing, ts)；
     * PRE_ACTIVE -> ACTIVATING；ACTIVE 仅刷新 last_login_time；BLOCKED/RETIRED 不更新。
     *
     * @param hsmUid           规范设备身份（tb_tbox.hsm）
     * @param ts               事件时间
     * @param preActiveStatus  PRE_ACTIVE 码值
     * @param activatingStatus ACTIVATING 码值
     * @param activeStatus     ACTIVE 码值
     * @return 更新行数
     */
    int recordLogin(@Param("hsmUid") String hsmUid,
                    @Param("ts") LocalDateTime ts,
                    @Param("preActiveStatus") Integer preActiveStatus,
                    @Param("activatingStatus") Integer activatingStatus,
                    @Param("activeStatus") Integer activeStatus);

    /**
     * 完成业务激活（幂等）
     * 仅当 device_status = ACTIVATING AND activate_time IS NULL 时更新为 ACTIVE 并写入 activate_time
     *
     * @param hsmUid           规范设备身份（tb_tbox.hsm）
     * @param activateTime     激活时间
     * @param activatingStatus ACTIVATING 码值
     * @param activeStatus     ACTIVE 码值
     * @return 更新行数
     */
    int completeActivation(@Param("hsmUid") String hsmUid,
                           @Param("activateTime") LocalDateTime activateTime,
                           @Param("activatingStatus") Integer activatingStatus,
                           @Param("activeStatus") Integer activeStatus);

}
