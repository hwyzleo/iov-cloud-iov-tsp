package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleSimPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 车辆SIM卡信息表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-06-29
 */
@Mapper
public interface VehicleSimMapper extends BaseDao<VehicleSimPo, Long> {

    /**
     * 根据VIN查询所有SIM卡
     *
     * @param vin 车架号
     * @return SIM卡列表
     */
    List<VehicleSimPo> selectByVin(@Param("vin") String vin);

    /**
     * 根据ICCID查询SIM卡
     *
     * @param iccid 集成电路卡识别码
     * @return SIM卡信息
     */
    VehicleSimPo selectByIccid(@Param("iccid") String iccid);

    /**
     * 条件更新（幂等）
     * 仅当 source_version 为空或 version > source_version 时更新
     *
     * @param iccid 集成电路卡识别码
     * @param simStatus SIM卡状态
     * @param realnameStatus 实名认证状态
     * @param version 数据源版本
     * @return 更新行数
     */
    int updateByIccidWithVersion(@Param("iccid") String iccid,
                                  @Param("simStatus") Integer simStatus,
                                  @Param("realnameStatus") Integer realnameStatus,
                                  @Param("version") Long version);

    /**
     * 根据VIN和卡槽更新ICCID
     *
     * @param vin 车架号
     * @param slotNo 卡槽编号
     * @param iccid 集成电路卡识别码
     * @return 更新行数
     */
    int updateIccidByVinAndSlot(@Param("vin") String vin,
                                 @Param("slotNo") Integer slotNo,
                                 @Param("iccid") String iccid);
}
