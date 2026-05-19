package net.hwyz.iov.cloud.iov.tsp.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.IdcmBaseException;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.VehicleHasBindIdcmException;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Idcm;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleIdcm;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleIdcmLog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.IdcmRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleIdcmLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleIdcmRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleIdcmDomainService {

    private final VehicleIdcmRepository vehicleIdcmRepository;
    private final VehicleIdcmLogRepository vehicleIdcmLogRepository;
    private final IdcmRepository idcmRepository;

    public VehicleIdcm get(String vin, String sn) {
        if (StrUtil.isNotBlank(vin)) {
            return vehicleIdcmRepository.getByVin(vin);
        } else if (StrUtil.isNotBlank(sn)) {
            return vehicleIdcmRepository.getBySn(sn);
        }
        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    public void bind(String vin, String sn) {
        Idcm idcm = idcmRepository.getBySn(sn);
        if (idcm == null) {
            throw new IdcmBaseException("信息娱乐模块[" + sn + "]不存在");
        }

        VehicleIdcm vehicleIdcm = vehicleIdcmRepository.getByVin(vin);
        if (vehicleIdcm == null) {
            vehicleIdcm = VehicleIdcm.builder().vin(vin).build();
        }

        if (vehicleIdcm.hasBindOtherIdcm(sn)) {
            throw new VehicleHasBindIdcmException(vin, vehicleIdcm.getSn(), sn);
        }

        if (vehicleIdcm.isAlreadyBindTo(sn)) {
            log.warn("车辆[{}]已绑定过信息娱乐模块[{}]", vin, sn);
            return;
        }

        vehicleIdcm.bindIdcm(sn);
        vehicleIdcmRepository.save(vehicleIdcm);

        recordLog(vehicleIdcm, "车辆绑定信息娱乐模块");
    }

    private void recordLog(VehicleIdcm vehicleIdcm, String remark) {
        VehicleIdcmLog vehicleIdcmLog = VehicleIdcmLog.builder()
                .vin(vehicleIdcm.getVin())
                .sn(vehicleIdcm.getSn())
                .description(remark)
                .build();
        vehicleIdcmLogRepository.save(vehicleIdcmLog);
    }

}