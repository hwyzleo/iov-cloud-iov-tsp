package net.hwyz.iov.cloud.iov.tsp.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleSim;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleSimLog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleSimRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleSimLogRepository;
import java.util.List;
import java.util.Optional;

/**
 * 车辆SIM卡投影领域服务
 * 处理CCS SIM状态变更事件的幂等投影
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleSimProjectionDomainService {

    private final VehicleSimRepository vehicleSimRepository;
    private final VehicleSimLogRepository vehicleSimLogRepository;

    /**
     * 应用SIM状态投影
     * 幂等处理：仅当 version 大于该行 source_version 才应用
     *
     * @param vin 车架号
     * @param iccid SIM卡号
     * @param simStatus SIM卡状态
     * @param realnameStatus 实名状态
     * @param version 事件版本号
     * @return 是否应用成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean applySimStatusProjection(String vin, String iccid, Integer simStatus, Integer realnameStatus, Long version) {
        log.info("应用SIM状态投影: vin={}, iccid={}, simStatus={}, realnameStatus={}, version={}",
                vin, iccid, simStatus, realnameStatus, version);

        // 参数校验
        if (vin == null || iccid == null || version == null) {
            log.warn("SIM状态投影参数不完整: vin={}, iccid={}, version={}", vin, iccid, version);
            return false;
        }

        // 查找对应卡行
        Optional<VehicleSim> vehicleSimOpt = vehicleSimRepository.getByIccid(iccid);

        if (vehicleSimOpt.isEmpty()) {
            log.warn("未找到对应SIM卡行: iccid={}, 尝试反查CCS对账", iccid);
            // 短暂竞态重试逻辑可在此处实现
            // 持续缺失/漂移走反查CCS对账（只读）
            return false;
        }

        VehicleSim vehicleSim = vehicleSimOpt.get();

        // VIN校验
        if (!vin.equals(vehicleSim.getVin())) {
            log.warn("VIN不匹配: 事件vin={}, 数据库vin={}, iccid={}", vin, vehicleSim.getVin(), iccid);
            return false;
        }

        // 通过实体领域方法应用投影（包含版本检查逻辑）
        boolean applied = vehicleSim.applySimStatusProjection(simStatus, realnameStatus, version);

        if (applied) {
            vehicleSimRepository.update(vehicleSim);

            // 记录审计日志
            VehicleSimLog logEntity = VehicleSimLog.builder()
                    .vin(vin)
                    .slotNo(vehicleSim.getSlotNo())
                    .iccid(iccid)
                    .simStatus(simStatus)
                    .realnameStatus(realnameStatus)
                    .packageCode(vehicleSim.getPackageCode())
                    .sourceVersion(version)
                    .description("CCS SIM状态变更投影")
                    .build();
            vehicleSimLogRepository.save(logEntity);

            log.info("SIM状态投影成功: vin={}, iccid={}, version={}", vin, iccid, version);
            return true;
        } else {
            log.info("SIM状态投影忽略(过期/重复): vin={}, iccid={}, version={}, 当前sourceVersion={}",
                    vin, iccid, version, vehicleSim.getSourceVersion());
            return false;
        }
    }

    /**
     * 处理绑定变更事件
     * 将ICCID写入对应卡槽
     *
     * @param vin 车架号
     * @param slotNo 卡槽序号
     * @param iccid SIM卡号
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleBindingChanged(String vin, Integer slotNo, String iccid) {
        log.info("处理绑定变更: vin={}, slotNo={}, iccid={}", vin, slotNo, iccid);

        // 查找或创建卡行
        List<VehicleSim> vehicleSims = vehicleSimRepository.getByVin(vin);
        Optional<VehicleSim> targetSim = vehicleSims.stream()
                .filter(sim -> sim.getSlotNo().equals(slotNo))
                .findFirst();

        if (targetSim.isPresent()) {
            // 更新已有卡槽的ICCID
            vehicleSimRepository.updateIccidByVinAndSlot(vin, slotNo, iccid);
            log.info("更新卡槽ICCID: vin={}, slotNo={}, iccid={}", vin, slotNo, iccid);
        } else {
            // 创建新卡行，利用数据库唯一约束防止并发重复插入
            try {
                VehicleSim newSim = VehicleSim.builder()
                        .vin(vin)
                        .slotNo(slotNo)
                        .iccid(iccid)
                        .build();
                newSim.initDefaultState();
                vehicleSimRepository.save(newSim);
                log.info("创建新卡行: vin={}, slotNo={}, iccid={}", vin, slotNo, iccid);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                log.info("ICCID并发插入冲突(已由其他线程创建): iccid={}, 跳过处理", iccid);
            }
        }
    }
}
