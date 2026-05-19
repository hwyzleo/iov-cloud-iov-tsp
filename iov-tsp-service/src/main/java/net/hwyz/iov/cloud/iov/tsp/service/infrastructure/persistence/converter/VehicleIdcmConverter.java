package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleIdcm;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleIdcmLog;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleIdcmLogPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleIdcmPo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VehicleIdcmConverter {

    VehicleIdcmPo toPo(VehicleIdcm vehicleIdcm);

    VehicleIdcm toEntity(VehicleIdcmPo po);

    VehicleIdcmLogPo toLogPo(VehicleIdcmLog vehicleIdcmLog);

}