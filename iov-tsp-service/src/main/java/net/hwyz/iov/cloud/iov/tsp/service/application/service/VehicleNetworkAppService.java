package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.VehicleNetworkHasExistException;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleNetworkDao;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleNetworkLogDao;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleNetworkLogPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleNetworkPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 车辆网联信息应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleNetworkAppService {

    private final VehicleNetworkDao vehicleNetworkDao;
    private final VehicleNetworkLogDao vehicleNetworkLogDao;

    /**
     * 创建车辆网联信息
     *
     * @param vehicleNetworkPo 车辆网联信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(VehicleNetworkPo vehicleNetworkPo) {
        if (ObjUtil.isNotNull(vehicleNetworkDao.selectByVin(vehicleNetworkPo.getVin()))) {
            throw new VehicleNetworkHasExistException(vehicleNetworkPo.getVin());
        }
        vehicleNetworkPo.setIccid1Online(false);
        vehicleNetworkPo.setIccid2Online(false);
        vehicleNetworkPo.setBinding(true);
        vehicleNetworkPo.setAuth(false);
        vehicleNetworkDao.insertPo(vehicleNetworkPo);
        recordLog(vehicleNetworkPo, "创建信息");
    }

    /**
     * 记录车辆网联信息变更日志
     *
     * @param vehicleNetworkPo 车辆网联信息对象
     * @param remark           变更备注
     */
    private void recordLog(VehicleNetworkPo vehicleNetworkPo, String remark) {
        vehicleNetworkLogDao.insertPo(VehicleNetworkLogPo.builder()
                .vin(vehicleNetworkPo.getVin())
                .iccid1(vehicleNetworkPo.getIccid1())
                .iccid2(vehicleNetworkPo.getIccid2())
                .packageCode(vehicleNetworkPo.getPackageCode())
                .binding(vehicleNetworkPo.getBinding())
                .auth(vehicleNetworkPo.getAuth())
                .description(remark)
                .build());
    }

}
