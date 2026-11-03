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
     * 根据HSM查询车联终端信息（LIMIT 1，兼容旧调用）
     *
     * @param hsm 硬件安全模块标识
     * @return 车联终端信息
     */
    TboxPo selectByHsm(String hsm);

    /**
     * 根据规范化 HSM 查询全部有效设备（不 LIMIT，用于检测 0/多条身份冲突）
     *
     * @param hsm 规范化硬件安全模块标识
     * @return 设备列表
     */
    java.util.List<TboxPo> selectAllByHsm(@Param("hsm") String hsm);

    /**
     * VIAM 投影：最近登录事实单调更新（IOV-TSP-DSN-CR-016）。
     * 唯一业务写入者=VIAM 安全投影处理链；last_login_time 取 max(existing, ts)（单调）；
     * 不写 first_login_time/device_status/activate_time（由快照或明确契约承载）。
     *
     * @param id          设备主键
     * @param ts          成功接入事实时间（occurredAt，UTC 墙钟）
     * @param projectedAt 投影更新时间（≠事实时间）
     * @param syncStatus  VIAM 投影同步状态
     * @return 更新行数
     */
    int applyViamLoginFact(@Param("id") Long id,
                           @Param("ts") LocalDateTime ts,
                           @Param("projectedAt") LocalDateTime projectedAt,
                           @Param("syncStatus") String syncStatus);

    /**
     * VIAM 投影：应用安全开通快照（IOV-TSP-DSN-CR-016）。
     * 以 VIAM 权威快照为准写入 device_status/first_login_time（set-once 权威值，不锁死本地旧值）；
     * last_login_time 单调 max（防乱序）；不写 activate_time（V-GAP-3：语义未明确，保持未知）。
     *
     * @param id             设备主键
     * @param deviceStatus   安全开通状态码（VIAM state 映射）
     * @param firstLoginTime 首次可信成功接入时间（VIAM 权威）
     * @param lastLoginTime  最近可信成功接入时间（VIAM 权威）
     * @param securityVersion VIAM 安全版本（V-GAP-1：当前快照未提供，null=未知）
     * @param projectedAt    投影更新时间
     * @param syncStatus     VIAM 投影同步状态
     * @return 更新行数
     */
    int applyViamSnapshot(@Param("id") Long id,
                          @Param("deviceStatus") Integer deviceStatus,
                          @Param("firstLoginTime") LocalDateTime firstLoginTime,
                          @Param("lastLoginTime") LocalDateTime lastLoginTime,
                          @Param("securityVersion") Long securityVersion,
                          @Param("projectedAt") LocalDateTime projectedAt,
                          @Param("syncStatus") String syncStatus);

}
