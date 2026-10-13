package net.hwyz.iov.cloud.iov.tsp.service.common.exception;

/**
 * 证书吊销依赖不可用异常（TSP-DSN-CR-011 §3/§6）
 * <p>
 * 所有无法获得确定 VALID/REVOKED 的 PKI 依赖结果（DNS、连接、TLS、超时、5xx/4xx、
 * 协议错误、配置错误、UNKNOWN）统一映射为本异常，必须保留原 cause；
 * 领域层据此 fail-closed 返回 DENY + DEPENDENCY_UNAVAILABLE。
 * <p>
 * {@code category} 用于指标 {@code tsp_pki_dependency_unavailable_total{category}}，
 * 只允许固定枚举，不得携带 endpoint/serial 等高基数字段。
 *
 * @author hwyz_leo
 */
public class CertificateDependencyUnavailableException extends DeviceAdmissionBaseException {

    /** 依赖分类：DNS 解析失败 **/
    public static final String CATEGORY_DNS = "dns";
    /** 依赖分类：连接失败 **/
    public static final String CATEGORY_CONNECT = "connect";
    /** 依赖分类：TLS/信任校验失败 **/
    public static final String CATEGORY_TLS = "tls";
    /** 依赖分类：读超时（结果未知） **/
    public static final String CATEGORY_TIMEOUT = "timeout";
    /** 依赖分类：协议/响应校验失败 **/
    public static final String CATEGORY_PROTOCOL = "protocol";
    /** 依赖分类：配置非法/未启用 **/
    public static final String CATEGORY_CONFIGURATION = "configuration";
    /** 依赖分类：结果未知 **/
    public static final String CATEGORY_UNKNOWN = "unknown";
    /** 依赖分类：内部错误 **/
    public static final String CATEGORY_INTERNAL = "internal";

    private final String category;

    public CertificateDependencyUnavailableException(String message) {
        this(CATEGORY_UNKNOWN, message, null);
    }

    public CertificateDependencyUnavailableException(String message, Throwable cause) {
        this(CATEGORY_UNKNOWN, message, cause);
    }

    public CertificateDependencyUnavailableException(String category, String message, Throwable cause) {
        super(207001, message);
        this.category = category == null ? CATEGORY_UNKNOWN : category;
        if (cause != null) {
            initCause(cause);
        }
    }

    public String getCategory() {
        return category;
    }
}
