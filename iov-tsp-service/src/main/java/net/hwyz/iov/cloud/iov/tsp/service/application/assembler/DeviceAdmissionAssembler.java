package net.hwyz.iov.cloud.iov.tsp.service.application.assembler;

import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.DeviceAdmissionCheckResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 设备接入准入装配器（TSP-DSN-CR-011 §4.3）
 * <p>
 * 领域枚举按同名映射为 API 枚举，禁止 API 反向依赖 service。
 *
 * @author hwyz_leo
 */
@Mapper
public interface DeviceAdmissionAssembler {

    DeviceAdmissionAssembler INSTANCE = Mappers.getMapper(DeviceAdmissionAssembler.class);

    @Mapping(source = "admissionResult", target = "decision")
    @Mapping(source = "reason", target = "reason")
    @Mapping(source = "vin", target = "vin")
    @Mapping(source = "bindStatus", target = "bindStatus")
    @Mapping(source = "certStatus", target = "certStatus")
    @Mapping(source = "deviceStatus", target = "deviceStatus")
    @Mapping(source = "bindVersion", target = "bindVersion")
    @Mapping(source = "updatedAt", target = "updatedAt")
    DeviceAdmissionCheckResult toResult(DeviceAdmission deviceAdmission);
}
