package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleCcp;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleCcpProjectionRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleCcpAppService {

    private final VehicleCcpProjectionRepository vehicleCcpProjectionRepository;

    public VehicleCcp get(String vin, String sn) {
        return vehicleCcpProjectionRepository.getByVinAndSn(vin, sn);
    }
}
