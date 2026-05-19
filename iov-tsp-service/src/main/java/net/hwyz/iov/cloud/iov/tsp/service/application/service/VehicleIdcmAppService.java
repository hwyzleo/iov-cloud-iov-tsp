package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.application.assembler.VehicleIdcmAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.VehicleIdcmBindCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.query.VehicleIdcmQuery;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.VehicleIdcmResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleIdcm;
import net.hwyz.iov.cloud.iov.tsp.service.domain.service.VehicleIdcmDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleIdcmAppService {

    private final VehicleIdcmDomainService vehicleIdcmDomainService;

    public VehicleIdcmResult get(VehicleIdcmQuery query) {
        VehicleIdcm vehicleIdcm = vehicleIdcmDomainService.get(query.getVin(), query.getSn());
        return VehicleIdcmAssembler.INSTANCE.toResult(vehicleIdcm);
    }

    @Transactional(rollbackFor = Exception.class)
    public void bind(VehicleIdcmBindCmd cmd) {
        vehicleIdcmDomainService.bind(cmd.getVin(), cmd.getSn());
    }

}