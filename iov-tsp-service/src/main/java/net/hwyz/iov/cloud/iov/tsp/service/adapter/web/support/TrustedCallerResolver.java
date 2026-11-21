package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.support;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.constant.CustomHeaders;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.common.util.ServletUtil;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.UserVehicleBindingErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 可信调用方上下文解析（IOV-TSP-DSN-CR-017 D017-01/D017-08）。
 * <p>
 * 账号与车辆身份只取平台既有可信边界：EDD-SGW 清洗客户端伪造头后按已验签的 CIAM 声明
 * 写入 {@code X-User-Id} / {@code X-Client-Type} / {@code X-Device-Id} / {@code X-Vin}，
 * 由框架 HeaderInterceptor 装入 SecurityContextHolder。TSP 不新建自定义身份头，
 * 也不接受请求体中的自报账号/VIN 作为权威。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class TrustedCallerResolver {

    private final TspUserVehicleProperties properties;

    /**
     * 解析 App 端可信账号（要求 App 客户端类型）
     *
     * @return 账号标识
     */
    public String requireAppAccount() {
        requireClientType(properties.getAppClientType());
        return requireAccountId();
    }

    /**
     * 设备主体前缀（CIAM 设备身份令牌 {@code sub=device:<principal>}）。
     * 设备令牌不得被当作账号使用（如 Account+VIN 隐私授权上报）。
     */
    private static final String DEVICE_SUBJECT_PREFIX = "device:";

    /**
     * 解析可信账号（不校验客户端类型，供共享入口使用）。
     * <p>
     * 拒绝设备主体：CIAM 设备令牌的 {@code sub} 形如 {@code device:<principal>}，
     * 若直接当账号会把设备主体写成 Account+VIN 的账号，必须 fail-closed。
     *
     * @return 账号标识
     */
    public String requireAccountId() {
        String userId = SecurityContextHolder.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(UserVehicleBindingErrorCode.INVALID_REQUEST);
        }
        String normalized = userId.trim();
        if (normalized.toLowerCase(java.util.Locale.ROOT).startsWith(DEVICE_SUBJECT_PREFIX)) {
            // 设备令牌只能用于车端能力，账号级接口必须使用用户令牌
            throw new BusinessException(UserVehicleBindingErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    /**
     * 解析可信车端上下文 VIN（要求车机客户端类型，且网关已注入 X-Vin）
     *
     * @param request HTTP 请求
     * @return 规范化 VIN
     */
    public String requireVehicleVin(HttpServletRequest request) {
        requireClientType(properties.getVehicleClientType());
        String vin = ServletUtil.getHeader(request, CustomHeaders.VIN);
        if (vin == null || vin.isBlank()) {
            throw new BusinessException(UserVehicleBindingErrorCode.INVALID_REQUEST);
        }
        return vin.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 可信车端引用（设备标识不可逆摘要，不含 PII）
     *
     * @return 车端引用；无设备标识时返回 null
     */
    public String trustedVehicleRef() {
        String deviceId = SecurityContextHolder.getDeviceId();
        if (deviceId == null || deviceId.isBlank()) {
            return null;
        }
        return "device:" + net.hwyz.iov.cloud.iov.tsp.service.common.util.DigestUtils.sha256Hex(deviceId.trim());
    }

    private void requireClientType(String expected) {
        String clientType = SecurityContextHolder.getClientType();
        if (clientType == null || clientType.isBlank()
                || expected == null || !clientType.trim().equalsIgnoreCase(expected)) {
            throw new BusinessException(UserVehicleBindingErrorCode.INVALID_REQUEST);
        }
    }
}
