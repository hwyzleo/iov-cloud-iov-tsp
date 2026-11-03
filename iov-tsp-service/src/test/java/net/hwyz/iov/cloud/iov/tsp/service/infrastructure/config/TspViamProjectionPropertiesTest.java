package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * VIAM 安全投影配置默认值测试（IOV-TSP-DSN-CR-016）
 * <p>
 * 默认关闭（联合切换后启用）；Topic 默认值对齐 VIAM 官方契约；
 * 独立 Consumer Group，不与旧安全消费者/远控共享。
 * </p>
 */
class TspViamProjectionPropertiesTest {

    @Test
    void defaults_ShouldMatchViamOfficialContractAndBeDisabled() {
        TspViamProjectionProperties properties = new TspViamProjectionProperties();

        assertFalse(properties.isEnabled(), "投影消费者默认关闭，旧 TSP 安全写链停用后启用");
        assertEquals("iov-tsp-viam-projection", properties.getConsumer().getGroupId());
        assertEquals("viam.device.login-succeeded", properties.getConsumer().getTopics().getLoginSucceeded());
        assertEquals("viam.device.onboarding.changed", properties.getConsumer().getTopics().getOnboardingChanged());
        assertEquals("viam.access-invalidated", properties.getConsumer().getTopics().getAccessInvalidated());
        assertEquals("viam.tbox-v2c-key-provisioned", properties.getConsumer().getTopics().getV2cKeyProvisioned());
        assertEquals(5, properties.getConsumer().getRetry().getMaxAttempts());
        assertEquals(3, properties.getSnapshot().getMaxAttempts());
    }
}
