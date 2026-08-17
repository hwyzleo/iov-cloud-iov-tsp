package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.application.assembler.VehicleAccessIdentityAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.VehicleAccessIdentityResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.vehicleAccessIdentity.service.VehicleAccessIdentityDomainService;
import org.springframework.stereotype.Service;

/**
 * 车辆接入身份反查应用服务
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleAccessIdentityAppService {

    private final VehicleAccessIdentityDomainService vehicleAccessIdentityDomainService;

    /**
     * 按 VIN 解析当前 TBOX 接入身份
     *
     * @param vin 车辆VIN
     * @return 接入身份解析结果
     */
    public VehicleAccessIdentityResult resolveByVin(String vin) {
        var result = vehicleAccessIdentityDomainService.resolveByVin(vin);
        return VehicleAccessIdentityAssembler.INSTANCE.toResult(result);
    }
}
