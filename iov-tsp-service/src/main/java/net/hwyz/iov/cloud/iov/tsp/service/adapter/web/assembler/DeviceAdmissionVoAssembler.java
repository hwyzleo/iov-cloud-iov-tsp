package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.api.vo.DeviceAdmissionCheckVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.DeviceAdmissionResultVo;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.DeviceAdmissionCheckCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.DeviceAdmissionCheckResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 设备接入准入VO装配器（TSP-DSN-CR-011）
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
    DeviceAdmissionResultVo toVo(DeviceAdmissionCheckResult result);
}
