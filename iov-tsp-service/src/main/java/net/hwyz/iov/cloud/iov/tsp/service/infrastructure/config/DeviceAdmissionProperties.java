package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 设备接入准入配置（TSP-DSN-CR-011 §8）
 * <p>
 * {@code tsp.device-admission.pki-revocation.enabled}：发布门禁，默认关闭仅用于发布编排。
 * 关闭时准入 readiness 为 NOT_READY，准入请求仍 fail-closed（不得跳过检查后 ALLOW）。
 *
 * @author hwyz_leo
 */
@Data
@Component
@ConfigurationProperties(prefix = "tsp.device-admission")
public class DeviceAdmissionProperties {

    /**
     * PKI 吊销检查发布门禁（默认关闭）
     */
    private PkiRevocation pkiRevocation = new PkiRevocation();

    @Data
    public static class PkiRevocation {
        /**
         * 是否启用 PKI 吊销检查（启用前必须完成 framework/配置/端到端验证）
         */
        private boolean enabled = false;
    }
}
