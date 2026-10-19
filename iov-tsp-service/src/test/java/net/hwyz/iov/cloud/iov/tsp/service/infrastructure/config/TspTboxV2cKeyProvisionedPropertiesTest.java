package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V2C 消费者配置测试（TSP-DSN-CR-012 §11）
 * <p>
 * 发布门禁默认关闭；迁移期旧键与新键冲突必须 fail-fast。
 */
class TspTboxV2cKeyProvisionedPropertiesTest {

    @Test
    void defaultGate_ShouldBeDisabled() {
        TspTboxV2cKeyProvisionedProperties properties = new TspTboxV2cKeyProvisionedProperties();
        assertFalse(properties.isEnabled());
        assertTrue(properties.isCurrentContractVersionSupported());
        assertDoesNotThrow(properties::validateLegacyGate);
    }

    @Test
    void legacyGateSameValue_ShouldBeTolerated() {
        TspTboxV2cKeyProvisionedProperties properties = new TspTboxV2cKeyProvisionedProperties();
        properties.setEnabled(true);
        properties.setLegacyV2cActivationEnabled(true);
        properties.setActiveProducerRelease("VMD-CR-062");

        assertDoesNotThrow(properties::validateLegacyGate);
    }

    @Test
    void legacyGateConflict_ShouldFailFast() {
        TspTboxV2cKeyProvisionedProperties properties = new TspTboxV2cKeyProvisionedProperties();
        properties.setEnabled(false);
        properties.setLegacyV2cActivationEnabled(true);

        assertThrows(IllegalStateException.class, properties::validateLegacyGate);
    }

    @Test
    void unsupportedContractVersion_ShouldBeDetected() {
        TspTboxV2cKeyProvisionedProperties properties = new TspTboxV2cKeyProvisionedProperties();
        properties.setSupportedContractVersions(java.util.Set.of("2"));

        assertFalse(properties.isCurrentContractVersionSupported());
    }

    @Test
    void enabledWithoutProducerRelease_ShouldFailFast() {
        TspTboxV2cKeyProvisionedProperties properties = new TspTboxV2cKeyProvisionedProperties();
        properties.setEnabled(true);

        assertThrows(IllegalStateException.class, properties::validateReleaseGate);
    }

    @Test
    void enabledWithDeclaredProducerRelease_ShouldPass() {
        TspTboxV2cKeyProvisionedProperties properties = new TspTboxV2cKeyProvisionedProperties();
        properties.setEnabled(true);
        properties.setActiveProducerRelease("VMD-CR-062");

        assertDoesNotThrow(properties::validateReleaseGate);
        assertTrue(properties.isProducerReleaseReady());
    }
}
