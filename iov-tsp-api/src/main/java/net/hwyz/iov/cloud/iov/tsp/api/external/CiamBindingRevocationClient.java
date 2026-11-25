package net.hwyz.iov.cloud.iov.tsp.api.external;

import net.hwyz.iov.cloud.iov.tsp.api.external.fallback.CiamBindingRevocationClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * SEC-CIAM 绑定关系解除后的凭证撤销契约（IOV-TSP-DSN-CR-021 D021-07）。
 * <p>
 * 对齐 SEC-CIAM-DSN-CR-007 D007-01 已实现契约：
 * {@code POST /api/service/ivi-user-session/v1/revoke-binding}，仅受信服务 {@code iov-tsp} 可调用
 * （框架 Feign 自动写入 {@code X-Caller-Service}）。CIAM 以 {@code SUCCEEDED + revokedSessionCount}
 * 表达目标撤销态已满足，同 operationId 重试或零匹配时数量为 0；技术失败通过错误信封表达，
 * 不伪造成功。TSP 侧只消费该幂等命令结果，不查询 CIAM 会话明细。
 * <p>
 * 响应信封字段与平台 {@code ApiResponse} 对齐，但本模块不直接依赖平台响应泛型，
 * 以避免 API 制品对框架实现类的额外耦合。
 *
 * @author hwyz_leo
 */
@FeignClient(name = "sec-ciam", contextId = "ciamBindingRevocationClient",
        path = "/api/service/ivi-user-session/v1",
        fallbackFactory = CiamBindingRevocationClientFallbackFactory.class)
public interface CiamBindingRevocationClient {

    /**
     * 按绑定关系撤销车机用户会话与 refresh（幂等）
     *
     * @param request 撤销请求（operationId 为 TSP 台账幂等键）
     * @return 平台响应信封
     */
    @PostMapping("/revoke-binding")
    RevokeBindingEnvelope revokeBinding(@RequestBody RevokeBindingRequest request);

    /**
     * 撤销请求（字段与 CIAM 契约一一对应）
     *
     * @param operationId TSP 解绑操作ID（幂等）
     * @param relationId  关系ID
     * @param accountId   账号标识
     * @param vin         车辆 VIN
     * @param occurredAt  关系释放绝对时刻（ISO-8601，带 Z/offset）
     * @param reason      原因码（可空，<=64）
     */
    record RevokeBindingRequest(
            String operationId,
            String relationId,
            String accountId,
            String vin,
            String occurredAt,
            String reason
    ) {
    }

    /**
     * 平台响应信封（只保留本契约关心的字段）
     *
     * @param code    错误码，"000000" 表示成功
     * @param message 描述
     * @param data    撤销结果
     */
    record RevokeBindingEnvelope(String code, String message, RevokeBindingData data) {
    }

    /**
     * 撤销结果
     *
     * @param operationId         TSP 解绑操作ID
     * @param relationId          关系ID
     * @param status              SUCCEEDED 表示目标撤销态已满足
     * @param revokedSessionCount 本次实际撤销会话数（重试可为 0）
     */
    record RevokeBindingData(
            String operationId,
            String relationId,
            String status,
            Integer revokedSessionCount
    ) {
    }
}
