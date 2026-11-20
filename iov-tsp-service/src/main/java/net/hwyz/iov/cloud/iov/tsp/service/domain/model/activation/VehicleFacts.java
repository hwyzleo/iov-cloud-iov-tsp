package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

/**
 * 车辆最小事实（来自 VMD 防腐层，只表达「车辆存在且可用」）。
 *
 * @author hwyz_leo
 */
public record VehicleFacts(boolean present, String vin) {

    public static VehicleFacts present(String vin) {
        return new VehicleFacts(true, vin);
    }

    public static VehicleFacts missing(String vin) {
        return new VehicleFacts(false, vin);
    }
}
