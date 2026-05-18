package net.hwyz.iov.cloud.iov.tsp.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.VehicleHasBindTboxException;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTboxLog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleTboxLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleTboxRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleTboxDomainService {

    private final VehicleTboxRepository vehicleTboxRepository;
    private final VehicleTboxLogRepository vehicleTboxLogRepository;

    public VehicleTbox get(String vin, String sn) {
        return vehicleTboxRepository.getByVinAndSn(vin, sn);
    }

    public void bind(String vin, String sn) {
        VehicleTbox vehicleTbox = vehicleTboxRepository.getByVin(vin);
        if (vehicleTbox == null) {
            vehicleTbox = VehicleTbox.builder()
                    .vin(vin)
                    .build();
        }
        if (StrUtil.isNotBlank(vehicleTbox.getSn())) {
            if (!vehicleTbox.getSn().equalsIgnoreCase(sn)) {
                throw new VehicleHasBindTboxException(vin, vehicleTbox.getSn(), sn);
            } else {
                log.warn("车辆[{}]在[{}]已绑定过车联终端[{}]", vin, vehicleTbox.getCreateTime(), sn);
                return;
            }
        }
        vehicleTbox.setSn(sn);
        if (vehicleTbox.getId() == null) {
            vehicleTboxRepository.save(vehicleTbox);
        } else {
            vehicleTboxRepository.update(vehicleTbox);
        }
        recordLog(vehicleTbox, "车辆绑定车联终端");
    }

    private void recordLog(VehicleTbox vehicleTbox, String remark) {
        VehicleTboxLog vehicleTboxLog = VehicleTboxLog.builder()
                .vin(vehicleTbox.getVin())
                .sn(vehicleTbox.getSn())
                .description(remark)
                .build();
        vehicleTboxLogRepository.save(vehicleTboxLog);
    }

}