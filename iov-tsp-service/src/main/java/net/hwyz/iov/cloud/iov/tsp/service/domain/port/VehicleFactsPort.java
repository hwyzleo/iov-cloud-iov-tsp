package net.hwyz.iov.cloud.iov.tsp.service.domain.port;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.VehicleFacts;

/**
 * 车辆最小事实端口（VMD 防腐层）。
 *
 * @author hwyz_leo
 */
public interface VehicleFactsPort {

    /**
     * 查询车辆是否存在且可用于绑车
     *
     * @param vin 车辆 VIN
     * @return 车辆最小事实
     * @throws DependencyUnavailableException 依赖不可用
     */
    VehicleFacts load(String vin);
}
