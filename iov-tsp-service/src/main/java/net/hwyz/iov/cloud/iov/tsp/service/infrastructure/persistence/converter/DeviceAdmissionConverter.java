package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmission;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.DeviceAdmissionLogPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 设备接入鉴权转换器
 *
 * @author hwyz_leo
 * @since 2025-05-14
 */
@Mapper(componentModel = "spring")
public interface DeviceAdmissionConverter {

    /**
     * 领域实体转日志持久化对象
     *
     * @param deviceAdmission 设备接入鉴权领域实体
     * @return 日志持久化对象
     */
    @Mapping(target = "admissionResult", expression = "java(deviceAdmission.getAdmissionResult() != null ? deviceAdmission.getAdmissionResult().getCode() : null)")
    @Mapping(target = "hsmCheckResult", expression = "java(deviceAdmission.getHsmCheckResult() != null ? deviceAdmission.getHsmCheckResult().getStatus().getCode() : null)")
    @Mapping(target = "pkiCheckResult", expression = "java(deviceAdmission.getPkiCheckResult() != null ? deviceAdmission.getPkiCheckResult().getStatus().getCode() : null)")
    @Mapping(target = "deviceStatusCheckResult", expression = "java(deviceAdmission.getDeviceStatusCheckResult() != null ? deviceAdmission.getDeviceStatusCheckResult().getStatus().getCode() : null)")
    @Mapping(target = "responseTimeMs", expression = "java(deviceAdmission.getResponseTimeMs() != null ? deviceAdmission.getResponseTimeMs().intValue() : null)")
    DeviceAdmissionLogPo toLogPo(DeviceAdmission deviceAdmission);
}
