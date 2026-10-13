package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.HsmUidNormalizer;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.DeviceAdmissionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleTboxMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleTboxPo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 设备接入准入仓储实现（TSP-DSN-CR-011 §3）
 * <p>
 * 设备事实读 tb_tbox；绑定解析读 VMD 绑定只读投影（device_category=TBOX 且 bind_state=ACTIVE），
 * 不 LIMIT 以便上层检测绑定冲突。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class DeviceAdmissionRepositoryImpl implements DeviceAdmissionRepository {

    private static final String DEVICE_CATEGORY_TBOX = "TBOX";

    private final TboxMapper tboxMapper;
    private final VehicleTboxMapper vehicleTboxMapper;

    @Override
    public Optional<TboxInfo> findByHsm(String hsm) {
        TboxPo tboxPo = tboxMapper.selectByHsm(HsmUidNormalizer.normalize(hsm));
        if (tboxPo == null) {
            return Optional.empty();
        }
        Instant modifyTime = tboxPo.getModifyTime() == null ? null : tboxPo.getModifyTime().toInstant();
        return Optional.of(new TboxInfo(
                tboxPo.getId(),
                tboxPo.getSn(),
                tboxPo.getHsm(),
                tboxPo.getDeviceStatus(),
                modifyTime
        ));
    }

    @Override
    public List<BindingInfo> findActiveBindingsBySn(String sn) {
        if (sn == null || sn.isEmpty()) {
            return List.of();
        }
        List<VehicleTboxPo> bindings = vehicleTboxMapper.selectActiveBySnAndCategory(sn, DEVICE_CATEGORY_TBOX);
        if (bindings == null || bindings.isEmpty()) {
            return List.of();
        }
        return bindings.stream()
                .map(po -> new BindingInfo(po.getVin(), po.getBindingVersion(), po.getLastEventTime()))
                .toList();
    }
}
