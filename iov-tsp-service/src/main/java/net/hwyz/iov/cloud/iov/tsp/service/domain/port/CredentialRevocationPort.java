package net.hwyz.iov.cloud.iov.tsp.service.domain.port;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.CredentialRevocationResult;

import java.time.Instant;

/**
 * 关系释放后的凭证撤销端口（IOV-TSP-DSN-CR-021 D021-07）。
 * <p>
 * 调用必须位于 TSP 本地关系事务与行锁之外；实现负责把超时、5xx、响应非法
 * 统一表达为 {@link DependencyUnavailableException}，不得返回伪成功。
 *
 * @author hwyz_leo
 */
public interface CredentialRevocationPort {

    /**
     * 撤销指定关系的车机用户会话与 refresh（幂等，沿用原 operationId）
     *
     * @param operationId 解绑操作ID（CIAM 幂等键）
     * @param relationId  关系ID
     * @param accountId   账号标识
     * @param vin         车辆 VIN
     * @param occurredAt  关系释放绝对时刻
     * @param reason      原因码（可空）
     * @return 撤销结果
     */
    CredentialRevocationResult revoke(String operationId, String relationId, String accountId, String vin,
                                      Instant occurredAt, String reason);
}
