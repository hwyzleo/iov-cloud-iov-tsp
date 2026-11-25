package net.hwyz.iov.cloud.iov.tsp.service.domain.port;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.FirstActivationFact;

/**
 * 整车首次激活事实只读端口（IOV-TSP-DSN-CR-021 D021-02）。
 * <p>
 * 仅用于 MPT 车辆详情受控读取，不得用于列表逐行调用；技术失败抛出
 * {@link DependencyUnavailableException}，由应用层映射为 UNKNOWN/PARTIAL。
 *
 * @author hwyz_leo
 */
public interface FirstActivationQueryPort {

    /**
     * 查询指定 VIN 的首次激活事实
     *
     * @param vin 车辆 VIN
     * @return 只读快照
     */
    FirstActivationFact load(String vin);
}
