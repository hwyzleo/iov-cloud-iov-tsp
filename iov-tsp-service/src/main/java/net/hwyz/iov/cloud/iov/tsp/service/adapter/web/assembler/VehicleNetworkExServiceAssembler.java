package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleNetworkExService;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleNetworkPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * 对外服务车联网联信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleNetworkExServiceAssembler {

    VehicleNetworkExServiceAssembler INSTANCE = Mappers.getMapper(VehicleNetworkExServiceAssembler.class);

    /**
     * 数据传输对象转数据对象
     *
     * @param vehicleNetworkExService 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VehicleNetworkPo toPo(VehicleNetworkExService vehicleNetworkExService);

}
