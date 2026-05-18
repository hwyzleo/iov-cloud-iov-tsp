package net.hwyz.iov.cloud.iov.tsp.service.domain.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleNetwork;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleNetworkLog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleNetworkLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleNetworkRepository;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.VehicleNetworkHasExistException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VehicleNetworkDomainService {

    private final VehicleNetworkRepository vehicleNetworkRepository;
    private final VehicleNetworkLogRepository vehicleNetworkLogRepository;

    public VehicleNetwork getByVin(String vin) {
        return vehicleNetworkRepository.getByVin(vin);
    }

    public void create(VehicleNetwork vehicleNetwork) {
        VehicleNetwork existing = getByVin(vehicleNetwork.getVin());
        if (existing != null) {
            throw new VehicleNetworkHasExistException(vehicleNetwork.getVin());
        }
        vehicleNetwork.initDefaultState();
        vehicleNetworkRepository.save(vehicleNetwork);
        recordLog(vehicleNetwork, "创建信息");
    }

    private void recordLog(VehicleNetwork vehicleNetwork, String remark) {
        VehicleNetworkLog log = VehicleNetworkLog.builder()
                .vin(vehicleNetwork.getVin())
                .iccid1(vehicleNetwork.getIccid1())
                .iccid2(vehicleNetwork.getIccid2())
                .packageCode(vehicleNetwork.getPackageCode())
                .binding(vehicleNetwork.getBinding())
                .auth(vehicleNetwork.getAuth())
                .description(remark)
                .build();
        vehicleNetworkLogRepository.save(log);
    }

}