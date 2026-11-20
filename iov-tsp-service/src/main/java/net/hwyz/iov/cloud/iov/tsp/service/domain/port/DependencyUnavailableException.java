package net.hwyz.iov.cloud.iov.tsp.service.domain.port;

/**
 * 外部依赖不可用（超时/故障/内容缺失）。
 * <p>
 * IOV-TSP-DSN-CR-017 D017-03：依赖不可用不得被当作「无事实」或「资格不符」，
 * 也不允许 fallback 放行；调用方映射为 DEPENDENCY_UNAVAILABLE 并拒绝写入。
 *
 * @author hwyz_leo
 */
public class DependencyUnavailableException extends RuntimeException {

    public DependencyUnavailableException(String message) {
        super(message);
    }

    public DependencyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
