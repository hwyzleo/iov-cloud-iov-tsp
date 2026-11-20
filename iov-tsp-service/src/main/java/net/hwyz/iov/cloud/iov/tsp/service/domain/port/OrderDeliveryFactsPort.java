package net.hwyz.iov.cloud.iov.tsp.service.domain.port;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.DeliveryFacts;

/**
 * 订单—交付事实端口（VSO 防腐层，只读同步核验）。
 *
 * @author hwyz_leo
 */
public interface OrderDeliveryFactsPort {

    /**
     * 以可信账号 + 会话 VIN 读取最小事实
     *
     * @param accountId 可信账号
     * @param vin       车辆 VIN
     * @return 归一化交付事实
     * @throws DependencyUnavailableException 依赖不可用（不得当作无订单/资格不符）
     */
    DeliveryFacts load(String accountId, String vin);
}
