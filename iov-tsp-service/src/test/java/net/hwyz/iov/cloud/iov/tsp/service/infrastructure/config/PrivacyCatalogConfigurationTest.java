package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.PrivacyAuthorizationCatalog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 隐私授权项目录装配测试（IOV-TSP-DSN-CR-020 TC020-01）。
 * <p>
 * 验证「配置驱动 + 启动校验」：空目录/版本缺失在 Bean 构建期即失败（应用启动失败），
 * 合法配置产出只读目录单例，日志侧只暴露版本与条目数量。
 */
class PrivacyCatalogConfigurationTest {

    private TspUserVehicleProperties properties(String catalogVersion, TspUserVehicleProperties.PrivacyAuthorization.Item... items) {
        TspUserVehicleProperties properties = new TspUserVehicleProperties();
        properties.getPrivacyAuthorization().setCatalogVersion(catalogVersion);
        properties.getPrivacyAuthorization().setItems(List.of(items));
        return properties;
    }

    private TspUserVehicleProperties.PrivacyAuthorization.Item item(String code, String zh, String en) {
        TspUserVehicleProperties.PrivacyAuthorization.Item item =
                new TspUserVehicleProperties.PrivacyAuthorization.Item();
        item.setItem(code);
        item.setDisplayName(zh);
        item.setDisplayNameEn(en);
        return item;
    }

    @Test
    @DisplayName("启动校验：空目录配置导致启动失败（fail-fast，不带空目录对外服务）")
    void emptyCatalogFailsStartup() {
        PrivacyAuthorizationCatalogConfiguration configuration =
                new PrivacyAuthorizationCatalogConfiguration(properties("v1"));

        assertThatThrownBy(configuration::privacyAuthorizationCatalog)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("目录为空");
    }

    @Test
    @DisplayName("启动校验：目录版本缺失导致启动失败")
    void missingCatalogVersionFailsStartup() {
        PrivacyAuthorizationCatalogConfiguration configuration =
                new PrivacyAuthorizationCatalogConfiguration(properties(null, item("GPS_LOCATION", "位置信息", null)));

        assertThatThrownBy(configuration::privacyAuthorizationCatalog)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("目录版本未配置");
    }

    @Test
    @DisplayName("合法配置：产出不可变目录单例（版本 + 条目 + 多语言展示名）")
    void validConfigurationBuildsCatalog() {
        PrivacyAuthorizationCatalogConfiguration configuration =
                new PrivacyAuthorizationCatalogConfiguration(properties("v1",
                        item("GPS_LOCATION", "位置信息", "Location Information")));

        PrivacyAuthorizationCatalog catalog = configuration.privacyAuthorizationCatalog();

        assertThat(catalog.catalogVersion()).isEqualTo("v1");
        assertThat(catalog.items()).hasSize(1);
        assertThat(catalog.localizedTo("en-US").items().get(0).displayName())
                .isEqualTo("Location Information");
        assertThat(catalog.localizedTo(null).items().get(0).displayName()).isEqualTo("位置信息");
    }
}
