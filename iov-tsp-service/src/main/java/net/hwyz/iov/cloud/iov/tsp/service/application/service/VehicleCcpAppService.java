package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.VehicleHasBindCcpException;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleCcpLogMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleCcpMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleCcpLogPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleCcpPo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 车辆中央计算平台相关应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleCcpAppService {

    private final VehicleCcpMapper vehicleCcpDao;
    private final VehicleCcpLogMapper vehicleCcpLogDao;

    /**
     * 根据车架号或序列号获取车辆中央计算平台
     *
     * @param sn 序列号
     * @return 车辆中央计算平台
     */
    public VehicleCcpPo get(String vin, String sn) {
        List<VehicleCcpPo> vehicleCcpPoList = vehicleCcpDao.selectPoByExample(VehicleCcpPo.builder().vin(vin).sn(sn).build());
        return vehicleCcpPoList.isEmpty() ? null : vehicleCcpPoList.get(0);
    }

    /**
     * 车辆绑定中央计算平台
     *
     * @param vin 车架号
     * @param sn  序列号
     */
    public void bind(String vin, String sn) {
        List<VehicleCcpPo> vehicleCcpPoList = vehicleCcpDao.selectPoByExample(VehicleCcpPo.builder().vin(vin).build());
        VehicleCcpPo vehicleCcpPo;
        if (vehicleCcpPoList.isEmpty()) {
            vehicleCcpPo = VehicleCcpPo.builder()
                    .vin(vin)
                    .build();
        } else {
            vehicleCcpPo = vehicleCcpPoList.get(0);
        }
        if (StrUtil.isNotBlank(vehicleCcpPo.getSn())) {
            if (!vehicleCcpPo.getSn().equalsIgnoreCase(sn)) {
                throw new VehicleHasBindCcpException(vin, vehicleCcpPo.getSn(), sn);
            } else {
                log.warn("车辆[{}]在[{}]已绑定过中央计算平台[{}]", vin, vehicleCcpPo.getCreateTime().getTime(), sn);
                return;
            }
        }
        vehicleCcpPo.setSn(sn);
        if (ObjUtil.isNull(vehicleCcpPo.getId())) {
            vehicleCcpDao.insertPo(vehicleCcpPo);
        } else {
            vehicleCcpDao.updatePo(vehicleCcpPo);
        }
        recordLog(vehicleCcpPo, "车辆绑定中央计算平台");
    }

    /**
     * 记录车辆中央计算平台变更日志
     *
     * @param vehicleCcpPo 车辆中央计算平台对象
     * @param remark       变更备注
     */
    private void recordLog(VehicleCcpPo vehicleCcpPo, String remark) {
        vehicleCcpLogDao.insertPo(VehicleCcpLogPo.builder()
                .vin(vehicleCcpPo.getVin())
                .sn(vehicleCcpPo.getSn())
                .description(remark)
                .build());
    }

}
