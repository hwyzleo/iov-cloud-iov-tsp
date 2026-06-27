package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository;

import java.util.Optional;

/**
 * 设备接入鉴权仓储接口
 *
 * @author hwyz_leo
 */
public interface DeviceAdmissionRepository {

    /**
     * 根据HSM查询TBOX信息
     * @param hsm 设备HSM标识
     * @return TBOX信息
     */
    Optional<TboxInfo> findByHsm(String hsm);

    /**
     * TBOX信息值对象
     */
    record TboxInfo(
        String vin,
        String hsm,
        Integer deviceStatus
    ) {}
}
