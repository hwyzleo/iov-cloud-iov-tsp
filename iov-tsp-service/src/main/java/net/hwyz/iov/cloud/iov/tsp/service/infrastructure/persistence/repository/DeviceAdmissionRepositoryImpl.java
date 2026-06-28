package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.DeviceAdmissionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleTboxMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleTboxPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 设备接入鉴权仓储实现
 *
 * @author hwyz_leo
 * @since 2025-05-14
 */
@Repository
@RequiredArgsConstructor
public class DeviceAdmissionRepositoryImpl implements DeviceAdmissionRepository {

    private final TboxMapper tboxMapper;
    private final VehicleTboxMapper vehicleTboxMapper;

    @Override
    public Optional<TboxInfo> findByHsm(String hsm) {
        TboxPo tboxPo = tboxMapper.selectByHsm(hsm);

        if (tboxPo == null) {
            return Optional.empty();
        }

        String vin = null;
        if (tboxPo.getSn() != null) {
            // 改读投影表，查询活跃绑定
            VehicleTboxPo vehicleTboxPo = vehicleTboxMapper.selectBySnAndBindState(tboxPo.getSn(), "ACTIVE");
            if (vehicleTboxPo != null) {
                vin = vehicleTboxPo.getVin();
            }
        }

        return Optional.of(new TboxInfo(
            vin,
            tboxPo.getHsm(),
            tboxPo.getDeviceStatus()
        ));
    }
}
