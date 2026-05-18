package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.VehicleHasBindTboxException;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleTboxMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleTboxLogMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleTboxLogPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleTboxPo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 车辆车联终端相关应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleTboxAppService {

    private final VehicleTboxMapper vehicleTboxMapper;
    private final VehicleTboxLogMapper vehicleTboxLogMapper;

    /**
     * 根据车架号或序列号获取车辆车联终端
     *
     * @param sn 序列号
     * @return 车辆车联终端
     */
    public VehicleTboxPo get(String vin, String sn) {
        List<VehicleTboxPo> vehicleTboxPoList = vehicleTboxMapper.selectPoByExample(VehicleTboxPo.builder().vin(vin).sn(sn).build());
        return vehicleTboxPoList.isEmpty() ? null : vehicleTboxPoList.get(0);
    }

    /**
     * 车辆绑定车联终端
     *
     * @param vin 车架号
     * @param sn  序列号
     */
    public void bind(String vin, String sn) {
        List<VehicleTboxPo> vehicleTboxPoList = vehicleTboxMapper.selectPoByExample(VehicleTboxPo.builder().vin(vin).build());
        VehicleTboxPo vehicleTboxPo;
        if (vehicleTboxPoList.isEmpty()) {
            vehicleTboxPo = VehicleTboxPo.builder()
                    .vin(vin)
                    .build();
        } else {
            vehicleTboxPo = vehicleTboxPoList.get(0);
        }
        if (StrUtil.isNotBlank(vehicleTboxPo.getSn())) {
            if (!vehicleTboxPo.getSn().equalsIgnoreCase(sn)) {
                throw new VehicleHasBindTboxException(vin, vehicleTboxPo.getSn(), sn);
            } else {
                log.warn("车辆[{}]在[{}]已绑定过车联终端[{}]", vin, vehicleTboxPo.getCreateTime().getTime(), sn);
                return;
            }
        }
        vehicleTboxPo.setSn(sn);
        if (ObjUtil.isNull(vehicleTboxPo.getId())) {
            vehicleTboxMapper.insertPo(vehicleTboxPo);
        } else {
            vehicleTboxMapper.updatePo(vehicleTboxPo);
        }
        recordLog(vehicleTboxPo, "车辆绑定车联终端");
    }

    /**
     * 记录车辆车联终端变更日志
     *
     * @param vehicleTboxPo 车辆车联终端对象
     * @param remark        变更备注
     */
    private void recordLog(VehicleTboxPo vehicleTboxPo, String remark) {
        vehicleTboxLogMapper.insertPo(VehicleTboxLogPo.builder()
                .vin(vehicleTboxPo.getVin())
                .sn(vehicleTboxPo.getSn())
                .description(remark)
                .build());
    }

}
