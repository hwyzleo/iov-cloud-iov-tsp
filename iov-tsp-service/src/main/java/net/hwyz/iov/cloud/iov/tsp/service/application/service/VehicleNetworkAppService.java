package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.application.assembler.VehicleNetworkAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.VehicleNetworkCmd;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleNetwork;
import net.hwyz.iov.cloud.iov.tsp.service.domain.service.VehicleNetworkDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleNetworkAppService {

    private final VehicleNetworkDomainService vehicleNetworkDomainService;

    @Transactional(rollbackFor = Exception.class)
    public void create(VehicleNetworkCmd cmd) {
        VehicleNetwork vehicleNetwork = VehicleNetworkAssembler.INSTANCE.toEntity(cmd);
        vehicleNetworkDomainService.create(vehicleNetwork);
    }

}