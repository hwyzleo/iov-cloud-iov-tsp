package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehiclePartBindingChangedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleCcp;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleTboxProjectionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleCcpProjectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleBindingProjectionAppService {

    private final VehicleTboxProjectionRepository vehicleTboxProjectionRepository;
    private final VehicleCcpProjectionRepository vehicleCcpProjectionRepository;

    /**
     * 处理 VMD 绑定变更事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleBindingChangedEvent(VehiclePartBindingChangedEvent event) {
        log.info("处理VMD绑定变更事件: vin={}, bindingId={}, changeType={}",
            event.getVin(), event.getBindingId(), event.getChangeType());

        if (!"TBOX".equals(event.getDeviceCategory()) && !"CCP".equals(event.getDeviceCategory())) {
            log.warn("忽略非TBOX/CCP设备类别: {}", event.getDeviceCategory());
            return;
        }

        if ("TBOX".equals(event.getDeviceCategory())) {
            handleTboxBindingChanged(event);
        } else {
            handleCcpBindingChanged(event);
        }
    }

    private void handleTboxBindingChanged(VehiclePartBindingChangedEvent event) {
        switch (event.getChangeType()) {
            case "BIND" -> {
                VehicleTbox tbox = buildTboxFromEvent(event);
                tbox.setBindState("ACTIVE");
                vehicleTboxProjectionRepository.upsertByBindingId(tbox);
            }
            case "UNBIND" -> {
                VehicleTbox existing = vehicleTboxProjectionRepository.getByBindingId(event.getBindingId());
                if (existing != null) {
                    existing.setBindState("INACTIVE");
                    existing.setBindingVersion(event.getSeq());
                    existing.setLastEventTime(event.getOccurredAt());
                    vehicleTboxProjectionRepository.upsertByBindingId(existing);
                }
            }
            case "REPLACE" -> {
                // 被替换的 binding 置 inactive
                if (event.getReplaceOfBindingId() != null) {
                    VehicleTbox replaced = vehicleTboxProjectionRepository.getByBindingId(event.getReplaceOfBindingId());
                    if (replaced != null) {
                        replaced.setBindState("INACTIVE");
                        replaced.setBindingVersion(event.getSeq());
                        replaced.setLastEventTime(event.getOccurredAt());
                        vehicleTboxProjectionRepository.upsertByBindingId(replaced);
                    }
                }
                // 新 binding 置 active
                VehicleTbox newTbox = buildTboxFromEvent(event);
                newTbox.setBindState("ACTIVE");
                vehicleTboxProjectionRepository.upsertByBindingId(newTbox);
            }
            default -> log.warn("未知的变更类型: {}", event.getChangeType());
        }
    }

    private void handleCcpBindingChanged(VehiclePartBindingChangedEvent event) {
        switch (event.getChangeType()) {
            case "BIND" -> {
                VehicleCcp ccp = buildCcpFromEvent(event);
                ccp.setBindState("ACTIVE");
                vehicleCcpProjectionRepository.upsertByBindingId(ccp);
            }
            case "UNBIND" -> {
                VehicleCcp existing = vehicleCcpProjectionRepository.getByBindingId(event.getBindingId());
                if (existing != null) {
                    existing.setBindState("INACTIVE");
                    existing.setBindingVersion(event.getSeq());
                    existing.setLastEventTime(event.getOccurredAt());
                    vehicleCcpProjectionRepository.upsertByBindingId(existing);
                }
            }
            case "REPLACE" -> {
                if (event.getReplaceOfBindingId() != null) {
                    VehicleCcp replaced = vehicleCcpProjectionRepository.getByBindingId(event.getReplaceOfBindingId());
                    if (replaced != null) {
                        replaced.setBindState("INACTIVE");
                        replaced.setBindingVersion(event.getSeq());
                        replaced.setLastEventTime(event.getOccurredAt());
                        vehicleCcpProjectionRepository.upsertByBindingId(replaced);
                    }
                }
                VehicleCcp newCcp = buildCcpFromEvent(event);
                newCcp.setBindState("ACTIVE");
                vehicleCcpProjectionRepository.upsertByBindingId(newCcp);
            }
            default -> log.warn("未知的变更类型: {}", event.getChangeType());
        }
    }

    private VehicleTbox buildTboxFromEvent(VehiclePartBindingChangedEvent event) {
        return VehicleTbox.builder()
            .vin(event.getVin())
            .sn(event.getSn())
            .bindingId(event.getBindingId())
            .partCode(event.getPartCode())
            .vehicleNodeCode(event.getVehicleNodeCode())
            .deviceCategory(event.getDeviceCategory())
            .bindingVersion(event.getSeq())
            .lastEventTime(event.getOccurredAt())
            .source("VMD")
            .build();
    }

    private VehicleCcp buildCcpFromEvent(VehiclePartBindingChangedEvent event) {
        return VehicleCcp.builder()
            .vin(event.getVin())
            .sn(event.getSn())
            .bindingId(event.getBindingId())
            .partCode(event.getPartCode())
            .vehicleNodeCode(event.getVehicleNodeCode())
            .deviceCategory(event.getDeviceCategory())
            .bindingVersion(event.getSeq())
            .lastEventTime(event.getOccurredAt())
            .source("VMD")
            .build();
    }

    /**
     * 启动时 bootstrap 对账
     */
    @Transactional(rollbackFor = Exception.class)
    public void bootstrap() {
        log.info("开始执行 bootstrap 对账");
        // TODO: 实现 bootstrap 逻辑，调用 VMD 接口拉取全量数据
    }

    /**
     * 定时对账
     */
    @Transactional(rollbackFor = Exception.class)
    public void reconcile() {
        log.info("开始执行定时对账");
        // TODO: 实现对账逻辑，比对本地投影与 VMD 数据
    }
}
