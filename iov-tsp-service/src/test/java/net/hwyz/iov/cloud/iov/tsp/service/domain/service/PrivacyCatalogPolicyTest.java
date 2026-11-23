package net.hwyz.iov.cloud.iov.tsp.service.domain.service;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.PrivacyAuthorizationCatalog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.PrivacyAuthorizationItem;
import net.hwyz.iov.cloud.iov.tsp.service.domain.service.PrivacyAuthorizationCatalogPolicy.RawItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 隐私授权项目录策略测试（IOV-TSP-DSN-CR-020 TC020-01，G-1 定稿）。
 * <p>
 * 覆盖：启动期 fail-fast（空目录/空版本/重复编码/非法编码/展示名为空）、
 * 目录版本匹配、目录外授权项、Accept-Language 本地化与回落。
 */
class PrivacyCatalogPolicyTest {

    private List<RawItem> validItems() {
        return List.of(
                new RawItem("GPS_LOCATION", "位置信息", "Location Information", false),
                new RawItem("DRIVING_BEHAVIOR", "驾驶行为分析", "Driving Behavior Analysis", true));
    }

    @Test
    @DisplayName("启动校验：目录为空即启动失败（不带空目录对外服务）")
    void emptyCatalogFailsStartup() {
        assertThatThrownBy(() -> PrivacyAuthorizationCatalogPolicy.build("v1", List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("目录为空");
        assertThatThrownBy(() -> PrivacyAuthorizationCatalogPolicy.build("v1", null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("启动校验：目录版本为空/空白即启动失败")
    void blankCatalogVersionFailsStartup() {
        assertThatThrownBy(() -> PrivacyAuthorizationCatalogPolicy.build(null, validItems()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("目录版本未配置");
        assertThatThrownBy(() -> PrivacyAuthorizationCatalogPolicy.build("  ", validItems()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("启动校验：编码重复/格式非法/展示名为空即启动失败")
    void invalidItemsFailStartup() {
        assertThatThrownBy(() -> PrivacyAuthorizationCatalogPolicy.build("v1", List.of(
                new RawItem("GPS_LOCATION", "位置信息", null, false),
                new RawItem("GPS_LOCATION", "重复", null, false))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("编码重复");

        assertThatThrownBy(() -> PrivacyAuthorizationCatalogPolicy.build("v1", List.of(
                new RawItem("gps_location", "位置信息", null, false))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("编码格式非法");

        assertThatThrownBy(() -> PrivacyAuthorizationCatalogPolicy.build("v1", List.of(
                new RawItem("GPS_LOCATION", " ", null, false))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("展示名不能为空");
    }

    @Test
    @DisplayName("目录构建：条目不含 currentVersion，顺序即配置顺序")
    void buildKeepsConfiguredItems() {
        PrivacyAuthorizationCatalog catalog = PrivacyAuthorizationCatalogPolicy.build("v1", validItems());

        assertThat(catalog.catalogVersion()).isEqualTo("v1");
        assertThat(catalog.items()).extracting(PrivacyAuthorizationItem::authorizationItem)
                .containsExactly("GPS_LOCATION", "DRIVING_BEHAVIOR");
        assertThat(catalog.items().get(1).required()).isTrue();
    }

    @Test
    @DisplayName("目录版本匹配：仅原样相等才通过（G-1 规则 1）")
    void versionMatchesExactly() {
        PrivacyAuthorizationCatalog catalog = PrivacyAuthorizationCatalogPolicy.build("v1", validItems());

        assertThat(catalog.versionMatches("v1")).isTrue();
        assertThat(catalog.versionMatches("v0")).isFalse();
        assertThat(catalog.versionMatches(null)).isFalse();
    }

    @Test
    @DisplayName("目录外授权项：返回未知编码列表（G-1 规则 2），不校验 version")
    void unknownItemsDetected() {
        PrivacyAuthorizationCatalog catalog = PrivacyAuthorizationCatalogPolicy.build("v1", validItems());

        assertThat(PrivacyAuthorizationCatalogPolicy.unknownItems(catalog,
                List.of("GPS_LOCATION", "UNKNOWN_ITEM"))).containsExactly("UNKNOWN_ITEM");
        assertThat(PrivacyAuthorizationCatalogPolicy.unknownItems(catalog,
                List.of("GPS_LOCATION", "DRIVING_BEHAVIOR"))).isEmpty();
    }

    @Test
    @DisplayName("Accept-Language：en-US 取英文展示名，缺省/不支持语言回落 zh-CN")
    void localizesByAcceptLanguage() {
        PrivacyAuthorizationCatalog catalog = PrivacyAuthorizationCatalogPolicy.build("v1", validItems());

        assertThat(catalog.localizedTo("en-US").items().get(0).displayName())
                .isEqualTo("Location Information");
        assertThat(catalog.localizedTo("en").items().get(0).displayName())
                .isEqualTo("Location Information");
        assertThat(catalog.localizedTo("en-US,en;q=0.9").items().get(0).displayName())
                .isEqualTo("Location Information");
        assertThat(catalog.localizedTo("zh-CN").items().get(0).displayName()).isEqualTo("位置信息");
        assertThat(catalog.localizedTo(null).items().get(0).displayName()).isEqualTo("位置信息");
        assertThat(catalog.localizedTo("fr-FR").items().get(0).displayName()).isEqualTo("位置信息");
        // 本地化不得改变目录版本与条目编码
        assertThat(catalog.localizedTo("en-US").catalogVersion()).isEqualTo("v1");
        assertThat(catalog.localizedTo("en-US").items().get(0).authorizationItem()).isEqualTo("GPS_LOCATION");
    }
}
