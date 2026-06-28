package net.hwyz.iov.cloud.iov.tsp.service.application.assembler;

import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.DeviceAdmissionCheckResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 设备接入鉴权装配器
 */
@Mapper
public interface DeviceAdmissionAssembler {

    DeviceAdmissionAssembler INSTANCE = Mappers.getMapper(DeviceAdmissionAssembler.class);

    @Mapping(source = "admissionResult.code", target = "admission")
    @Mapping(source = "hsmCheckResult.status.code", target = "bindStatus")
    @Mapping(source = "pkiCheckResult.status.code", target = "certStatus")
    @Mapping(source = "deviceStatusCheckResult.status.code", target = "deviceStatus")
    @Mapping(source = "createTime", target = "updatedAt")
    @Mapping(target = "bindVersion", ignore = true)
    @Mapping(target = "checkDetails", ignore = true)
    DeviceAdmissionCheckResult toResult(DeviceAdmission deviceAdmission);
}
