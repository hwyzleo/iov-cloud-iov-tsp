package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.api.vo.DeviceAdmissionCheckVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.DeviceAdmissionResultVo;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.DeviceAdmissionCheckCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.DeviceAdmissionCheckResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * 设备接入鉴权VO装配器
 *
 * @author hwyz_leo
 */
@Mapper
public interface DeviceAdmissionVoAssembler {

    DeviceAdmissionVoAssembler INSTANCE = Mappers.getMapper(DeviceAdmissionVoAssembler.class);

    /**
     * 请求VO转命令DTO
     */
    DeviceAdmissionCheckCmd toCmd(DeviceAdmissionCheckVo checkVo);

    /**
     * 结果DTO转响应VO
     */
    @Mapping(source = ".", target = "checkDetails", qualifiedByName = "toCheckDetailsVo")
    DeviceAdmissionResultVo toVo(DeviceAdmissionCheckResult result);

    @Named("toCheckDetailsVo")
    default DeviceAdmissionResultVo.CheckDetails toCheckDetailsVo(DeviceAdmissionCheckResult result) {
        if (result == null || result.getCheckDetails() == null) {
            return null;
        }

        DeviceAdmissionCheckResult.CheckDetails details = result.getCheckDetails();
        return DeviceAdmissionResultVo.CheckDetails.builder()
            .hsmCheck(toCheckResultVo(details.getHsmCheck()))
            .pkiCheck(toCheckResultVo(details.getPkiCheck()))
            .deviceStatusCheck(toCheckResultVo(details.getDeviceStatusCheck()))
            .build();
    }

    default DeviceAdmissionResultVo.CheckResult toCheckResultVo(DeviceAdmissionCheckResult.CheckResult checkResult) {
        if (checkResult == null) {
            return null;
        }

        return DeviceAdmissionResultVo.CheckResult.builder()
            .status(checkResult.getStatus())
            .message(checkResult.getMessage())
            .build();
    }
}
