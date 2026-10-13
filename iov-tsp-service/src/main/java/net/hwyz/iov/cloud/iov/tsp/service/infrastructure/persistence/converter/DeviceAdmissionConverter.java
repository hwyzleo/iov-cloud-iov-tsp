package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmission;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.DeviceAdmissionLogPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 设备接入鉴权转换器（TSP-DSN-CR-011 §9/§10）
 * <p>
 * 审计只落不可逆摘要；旧列 hsm 不再写入明文。
 *
 * @author hwyz_leo
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
    @Mapping(target = "reason", expression = "java(deviceAdmission.getReason() != null ? deviceAdmission.getReason().name() : null)")
    @Mapping(target = "hsmCheckResult", source = "uidCheckResult")
    @Mapping(target = "pkiCheckResult", expression = "java(deviceAdmission.getCertStatus() != null ? deviceAdmission.getCertStatus().name() : null)")
    @Mapping(target = "deviceStatusCheckResult", source = "deviceStatusCheckResult")
    @Mapping(target = "hsm", ignore = true)
    @Mapping(target = "responseTimeMs", expression = "java(deviceAdmission.getResponseTimeMs() != null ? deviceAdmission.getResponseTimeMs().intValue() : null)")
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "modifyBy", ignore = true)
    DeviceAdmissionLogPo toLogPo(DeviceAdmission deviceAdmission);
}
