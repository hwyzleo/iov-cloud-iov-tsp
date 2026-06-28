package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.application.assembler.VehicleTboxAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.VehicleTboxResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.service.VehicleTboxDomainService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleTboxAppService {

    private final VehicleTboxDomainService vehicleTboxDomainService;

    public VehicleTboxResult get(String vin, String sn) {
        VehicleTbox vehicleTbox = vehicleTboxDomainService.get(vin, sn);
        return VehicleTboxAssembler.INSTANCE.toResult(vehicleTbox);
    }
}
