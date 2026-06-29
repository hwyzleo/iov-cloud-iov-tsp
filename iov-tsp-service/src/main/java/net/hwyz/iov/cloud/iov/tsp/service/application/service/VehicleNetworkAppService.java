package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleNetworkVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleSimVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.event.CcsSimStatusChangedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.VehicleNetworkCmd;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.VehicleNetworkHasExistException;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleSim;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleSimRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.service.VehicleSimProjectionDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleNetworkAppService {

    private final VehicleSimRepository vehicleSimRepository;
    private final VehicleSimProjectionDomainService vehicleSimProjectionDomainService;

    /**
     * 处理SIM状态变更事件
     *
     * @param event 翻译后的CCS SIM状态变更事件
     * @return 是否处理成功
     */
    public boolean handleSimStatusChanged(CcsSimStatusChangedEvent event) {
        log.info("处理SIM状态变更事件: vin={}, iccid={}, version={}",
                event.getVin(), event.getIccid(), event.getVersion());

        return vehicleSimProjectionDomainService.applySimStatusProjection(
                event.getVin(),
                event.getIccid(),
                event.getSimStatus(),
                event.getRealnameStatus(),
                event.getVersion()
        );
    }

    /**
     * 处理绑定变更事件
     * 将ICCID写入对应卡槽
     *
     * @param vin 车架号
     * @param slotNo 卡槽序号
     * @param iccid SIM卡号
     */
    public void handleBindingChanged(String vin, Integer slotNo, String iccid) {
        log.info("处理绑定变更事件: vin={}, slotNo={}, iccid={}", vin, slotNo, iccid);
        vehicleSimProjectionDomainService.handleBindingChanged(vin, slotNo, iccid);
    }

    /**
     * 根据VIN获取车辆网络信息（包含SIM卡列表）
     *
     * @param vin 车架号
     * @return 车辆网络VO（包含SIM卡列表）
     */
    public VehicleNetworkVo getByVin(String vin) {
        log.info("根据VIN获取车辆网络信息: vin={}", vin);

        List<VehicleSim> vehicleSims = vehicleSimRepository.getByVin(vin);

        List<VehicleSimVo> simVoList = vehicleSims.stream()
                .map(sim -> VehicleSimVo.builder()
                        .slotNo(sim.getSlotNo())
                        .iccid(sim.getIccid())
                        .simStatus(sim.getSimStatus())
                        .realnameStatus(sim.getRealnameStatus())
                        .packageCode(sim.getPackageCode())
                        .build())
                .toList();

        return VehicleNetworkVo.builder()
                .vin(vin)
                .simList(simVoList)
                .build();
    }

    /**
     * 创建车辆网络信息（按卡子表）
     *
     * @param cmd 车辆网络命令
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(VehicleNetworkCmd cmd) {
        log.info("创建车辆网络信息: vin={}", cmd.getVin());

        // 检查是否已存在
        List<VehicleSim> existingSims = vehicleSimRepository.getByVin(cmd.getVin());
        if (!existingSims.isEmpty()) {
            throw new VehicleNetworkHasExistException(cmd.getVin());
        }

        // 创建SIM卡行
        if (cmd.getSimList() != null) {
            for (VehicleNetworkCmd.VehicleSimCmd simCmd : cmd.getSimList()) {
                VehicleSim vehicleSim = VehicleSim.builder()
                        .vin(cmd.getVin())
                        .slotNo(simCmd.getSlotNo())
                        .iccid(simCmd.getIccid())
                        .description(cmd.getDescription())
                        .build();
                vehicleSim.initDefaultState();
                vehicleSimRepository.save(vehicleSim);
            }
        }
    }

}