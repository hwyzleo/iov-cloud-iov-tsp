package net.hwyz.iov.cloud.iov.tsp.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleTboxProjectionRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleTboxDomainService {

    private final VehicleTboxProjectionRepository vehicleTboxProjectionRepository;

    public VehicleTbox get(String vin, String sn) {
        return vehicleTboxProjectionRepository.getByVinAndSn(vin, sn);
    }
}
