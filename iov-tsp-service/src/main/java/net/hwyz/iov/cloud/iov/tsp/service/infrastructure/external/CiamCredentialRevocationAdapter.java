package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.external.CiamBindingRevocationClient;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.CredentialRevocationResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.CredentialRevocationPort;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.DependencyUnavailableException;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * SEC-CIAM 绑定撤销防腐适配（IOV-TSP-DSN-CR-021 D021-07）。
 * <p>
 * 只有 CIAM 明确返回成功信封且 {@code status=SUCCEEDED} 才视为撤销收敛；
 * 超时、技术失败、响应非法或非成功码一律抛依赖不可用，由台账记录 PENDING/FAILED，
 * 绝不伪造成功。日志不记录账号、VIN 或任何令牌。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CiamCredentialRevocationAdapter implements CredentialRevocationPort {

    private static final String SUCCESS_CODE = "000000";
    private static final String STATUS_SUCCEEDED = "SUCCEEDED";

    private final CiamBindingRevocationClient ciamBindingRevocationClient;

    @Override
    public CredentialRevocationResult revoke(String operationId, String relationId, String accountId, String vin,
                                             Instant occurredAt, String reason) {
        CiamBindingRevocationClient.RevokeBindingEnvelope envelope;
        try {
            envelope = ciamBindingRevocationClient.revokeBinding(new CiamBindingRevocationClient.RevokeBindingRequest(
                    operationId, relationId, accountId, vin,
                    occurredAt == null ? null : occurredAt.toString(), reason));
        } catch (Exception e) {
            // 超时/连接失败/降级：关系已释放，凭证撤销交由台账重试
            log.warn("CIAM 绑定撤销调用失败: operationId={}, error={}", operationId, e.getClass().getSimpleName());
            throw new DependencyUnavailableException("CIAM 绑定撤销调用失败", e);
        }
        if (envelope == null || !SUCCESS_CODE.equals(envelope.code()) || envelope.data() == null
                || !STATUS_SUCCEEDED.equals(envelope.data().status())) {
            String code = envelope == null ? null : envelope.code();
            log.warn("CIAM 绑定撤销未确认成功: operationId={}, code={}", operationId, code);
            throw new DependencyUnavailableException("CIAM 绑定撤销未确认成功");
        }
        Integer count = envelope.data().revokedSessionCount();
        return new CredentialRevocationResult(operationId, relationId, count == null ? 0 : count);
    }
}
