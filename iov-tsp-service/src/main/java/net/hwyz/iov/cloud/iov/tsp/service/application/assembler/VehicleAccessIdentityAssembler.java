package net.hwyz.iov.cloud.iov.tsp.service.application.assembler;

import net.hwyz.iov.cloud.iov.tsp.service.domain.vehicleAccessIdentity.model.VehicleAccessIdentityResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 车辆接入身份解析装配器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleAccessIdentityAssembler {

    VehicleAccessIdentityAssembler INSTANCE = Mappers.getMapper(VehicleAccessIdentityAssembler.class);

    /**
     * 领域结果转应用结果（枚举自动映射为 name）
     */
    net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.VehicleAccessIdentityResult toResult(VehicleAccessIdentityResult result);
}
