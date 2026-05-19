package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.VehicleHasBindIdcmException;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleIdcmLogMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleIdcmMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleIdcmLogPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleIdcmPo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 车辆信息娱乐模块相关应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleIdcmAppService {

    private final VehicleIdcmMapper vehicleIdcmDao;
    private final VehicleIdcmLogMapper vehicleIdcmLogDao;

    /**
     * 根据车架号或序列号获取车辆信息娱乐模块
     *
     * @param sn 序列号
     * @return 车辆信息娱乐模块
     */
    public VehicleIdcmPo get(String vin, String sn) {
        List<VehicleIdcmPo> vehicleIdcmPoList = vehicleIdcmDao.selectPoByExample(VehicleIdcmPo.builder().vin(vin).sn(sn).build());
        return vehicleIdcmPoList.isEmpty() ? null : vehicleIdcmPoList.get(0);
    }

    /**
     * 车辆绑定信息娱乐模块
     *
     * @param vin 车架号
     * @param sn  序列号
     */
    public void bind(String vin, String sn) {
        List<VehicleIdcmPo> vehicleIdcmPoList = vehicleIdcmDao.selectPoByExample(VehicleIdcmPo.builder().vin(vin).build());
        VehicleIdcmPo vehicleIdcmPo;
        if (vehicleIdcmPoList.isEmpty()) {
            vehicleIdcmPo = VehicleIdcmPo.builder()
                    .vin(vin)
                    .build();
        } else {
            vehicleIdcmPo = vehicleIdcmPoList.get(0);
        }
        if (StrUtil.isNotBlank(vehicleIdcmPo.getSn())) {
            if (!vehicleIdcmPo.getSn().equalsIgnoreCase(sn)) {
                throw new VehicleHasBindIdcmException(vin, vehicleIdcmPo.getSn(), sn);
            } else {
                log.warn("车辆[{}]在[{}]已绑定过信息娱乐模块[{}]", vin, vehicleIdcmPo.getCreateTime().getTime(), sn);
                return;
            }
        }
        vehicleIdcmPo.setSn(sn);
        if (ObjUtil.isNull(vehicleIdcmPo.getId())) {
            vehicleIdcmDao.insertPo(vehicleIdcmPo);
        } else {
            vehicleIdcmDao.updatePo(vehicleIdcmPo);
        }
        recordLog(vehicleIdcmPo, "车辆绑定信息娱乐模块");
    }

    /**
     * 记录车辆信息娱乐模块变更日志
     *
     * @param vehicleIdcmPo 车辆信息娱乐模块对象
     * @param remark        变更备注
     */
    private void recordLog(VehicleIdcmPo vehicleIdcmPo, String remark) {
        vehicleIdcmLogDao.insertPo(VehicleIdcmLogPo.builder()
                .vin(vehicleIdcmPo.getVin())
                .sn(vehicleIdcmPo.getSn())
                .description(remark)
                .build());
    }

}
