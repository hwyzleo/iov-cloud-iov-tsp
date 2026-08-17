package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleAccessIdentityResultVo;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.VehicleAccessIdentityResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 车辆接入身份反查VO装配器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleAccessIdentityVoAssembler {

    VehicleAccessIdentityVoAssembler INSTANCE = Mappers.getMapper(VehicleAccessIdentityVoAssembler.class);

    /**
     * 应用结果转响应VO
     */
    VehicleAccessIdentityResultVo toVo(VehicleAccessIdentityResult result);
}
