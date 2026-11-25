package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

/**
 * CIAM 绑定关系撤销命令结果（IOV-TSP-DSN-CR-021 D021-07）。
 * <p>
 * {@code SUCCEEDED} 表示目标撤销态已满足（重复重试或零匹配时数量为 0）；
 * 技术失败以异常表达，不得构造伪成功结果。
 *
 * @param operationId         TSP 解绑操作ID
 * @param relationId          关系ID
 * @param revokedSessionCount 本次实际撤销会话数
 * @author hwyz_leo
 */
public record CredentialRevocationResult(String operationId, String relationId, int revokedSessionCount) {
}
