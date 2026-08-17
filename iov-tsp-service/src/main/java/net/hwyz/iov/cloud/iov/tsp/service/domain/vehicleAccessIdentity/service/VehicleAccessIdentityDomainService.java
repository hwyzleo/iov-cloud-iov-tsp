package net.hwyz.iov.cloud.iov.tsp.service.domain.vehicleAccessIdentity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ResolveReason;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ResolveStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Tbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleTboxProjectionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.vehicleAccessIdentity.model.VehicleAccessIdentityResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 车辆接入身份反查领域服务
 *
 * <p>VIN → 当前 ACTIVE 车辆-TBOX绑定投影 → TBOX SN → tb_tbox.hsm 的原子解析（fail-closed）。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleAccessIdentityDomainService {

    private final VehicleTboxProjectionRepository vehicleTboxProjectionRepository;
    private final TboxRepository tboxRepository;

    /**
     * 按 VIN 解析当前 TBOX 接入身份（fail-closed）
     *
     * @param vin 车辆VIN
     * @return 接入身份解析结果
     */
    public VehicleAccessIdentityResult resolveByVin(String vin) {
        long startTime = System.currentTimeMillis();

        // 1. 校验 VIN 非空
        if (StrUtil.isBlank(vin)) {
            log.warn("车辆接入身份反查: VIN为空");
            return unresolved(vin, ResolveReason.VIN_UNKNOWN);
        }

        try {
            // 2. 查询该 VIN 当前 ACTIVE TBOX 绑定投影
            List<VehicleTbox> activeBindings = vehicleTboxProjectionRepository
                    .listActiveTboxBindingsByVin(vin);

            // 3. 无 ACTIVE 绑定 -> UNBOUND；多条 -> BINDING_CONFLICT（fail-closed，不随机选择）
            if (activeBindings.isEmpty()) {
                log.warn("车辆接入身份反查: 未绑定, vin={}", maskVin(vin));
                return unresolved(vin, ResolveReason.UNBOUND);
            }
            if (activeBindings.size() > 1) {
                log.error("车辆接入身份反查: 绑定冲突, vin={}, 绑定数={}", maskVin(vin), activeBindings.size());
                return unresolved(vin, ResolveReason.BINDING_CONFLICT);
            }

            VehicleTbox binding = activeBindings.get(0);

            // 4. 使用投影中的 sn 查询 TBOX 设备
            Tbox tbox = tboxRepository.getBySn(binding.getSn());
            if (tbox == null) {
                log.warn("车辆接入身份反查: TBOX设备缺失, vin={}, sn={}", maskVin(vin), binding.getSn());
                return unresolved(vin, ResolveReason.TBOX_NOT_FOUND);
            }

            // 5. HSM UID 缺失 -> fail-closed
            if (StrUtil.isBlank(tbox.getHsm())) {
                log.warn("车辆接入身份反查: HSM UID缺失, vin={}, sn={}", maskVin(vin), binding.getSn());
                return unresolved(vin, ResolveReason.HSM_UID_MISSING);
            }

            // 6. 解析成功
            log.info("车辆接入身份反查成功: vin={}, hsmUid={}, tboxSn={}, bindingVersion={}, time={}ms",
                    maskVin(vin), maskHsm(tbox.getHsm()), binding.getSn(),
                    binding.getBindingVersion(), System.currentTimeMillis() - startTime);

            return VehicleAccessIdentityResult.builder()
                    .vin(vin)
                    .status(ResolveStatus.RESOLVED)
                    .reason(ResolveReason.RESOLVED)
                    .hsmUid(tbox.getHsm())
                    .tboxSn(binding.getSn())
                    .bindingVersion(binding.getBindingVersion())
                    .bindingUpdatedAt(binding.getLastEventTime())
                    .build();
        } catch (Exception e) {
            // 数据库/依赖异常：fail-closed，禁止返回伪身份
            log.error("车辆接入身份反查异常, vin={}", maskVin(vin), e);
            return unresolved(vin, ResolveReason.DEPENDENCY_UNAVAILABLE);
        }
    }

    private VehicleAccessIdentityResult unresolved(String vin, ResolveReason reason) {
        return VehicleAccessIdentityResult.builder()
                .vin(vin)
                .status(ResolveStatus.UNRESOLVED)
                .reason(reason)
                .build();
    }

    private String maskVin(String vin) {
        if (StrUtil.isBlank(vin) || vin.length() < 8) {
            return vin;
        }
        return vin.substring(0, 4) + "****" + vin.substring(vin.length() - 4);
    }

    private String maskHsm(String hsm) {
        if (StrUtil.isBlank(hsm) || hsm.length() < 8) {
            return hsm;
        }
        return hsm.substring(0, 4) + "****" + hsm.substring(hsm.length() - 4);
    }
}
