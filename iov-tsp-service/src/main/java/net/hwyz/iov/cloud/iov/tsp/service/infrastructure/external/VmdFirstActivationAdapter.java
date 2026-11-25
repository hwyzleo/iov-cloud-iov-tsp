package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.external.VmdVehicleActivationClient;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.FirstActivationState;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.FirstActivationFact;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.DependencyUnavailableException;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.FirstActivationQueryPort;
import org.springframework.stereotype.Component;

/**
 * VMD 整车首次激活事实只读防腐适配（IOV-TSP-DSN-CR-021 D021-02）。
 * <p>
 * 三态结论原样映射；技术失败抛依赖不可用，由应用层映射为 UNKNOWN/PARTIAL，
 * 不使用 TSP Outbox 的 PUBLISHED 状态冒充 VMD RECORDED。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VmdFirstActivationAdapter implements FirstActivationQueryPort {

    private final VmdVehicleActivationClient vmdVehicleActivationClient;

    @Override
    public FirstActivationFact load(String vin) {
        VmdVehicleActivationClient.FirstActivationDto dto;
        try {
            dto = vmdVehicleActivationClient.getFirstActivation(vin);
        } catch (Exception e) {
            log.warn("VMD 首次激活事实查询失败: vin={}, error={}", maskVin(vin), e.getClass().getSimpleName());
            throw new DependencyUnavailableException("VMD 首次激活事实查询不可用", e);
        }
        if (dto == null || dto.state() == null) {
            throw new DependencyUnavailableException("VMD 首次激活事实响应非法");
        }
        FirstActivationState state = parseState(dto.state());
        if (state == null) {
            throw new DependencyUnavailableException("VMD 首次激活事实状态非法");
        }
        return new FirstActivationFact(state, dto.firstActivatedAt(), dto.factRef());
    }

    private FirstActivationState parseState(String value) {
        try {
            FirstActivationState state = FirstActivationState.valueOf(value);
            // UNKNOWN 是本地故障语义，不接受外部直接声明
            return state == FirstActivationState.UNKNOWN ? null : state;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String maskVin(String vin) {
        if (vin == null || vin.length() <= 7) {
            return "***";
        }
        return vin.substring(0, 3) + "***" + vin.substring(vin.length() - 4);
    }
}
