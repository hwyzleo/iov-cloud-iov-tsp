package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleIdcmVo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleIdcmPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * 对外服务车辆信息娱乐模块转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleIdcmExServiceAssembler {

    VehicleIdcmExServiceAssembler INSTANCE = Mappers.getMapper(VehicleIdcmExServiceAssembler.class);

    /**
     * 数据传输对象转数据对象
     *
     * @param vehicleIdcmExService 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VehicleIdcmPo toPo(VehicleIdcmVo vehicleIdcmExService);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehicleIdcmPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    VehicleIdcmVo fromPo(VehicleIdcmPo vehicleIdcmPo);

}
