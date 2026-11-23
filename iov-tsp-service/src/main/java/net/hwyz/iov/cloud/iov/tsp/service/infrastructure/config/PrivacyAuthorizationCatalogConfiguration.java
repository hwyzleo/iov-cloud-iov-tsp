package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.PrivacyAuthorizationCatalog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.service.PrivacyAuthorizationCatalogPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 隐私授权项目录装配（IOV-TSP-DSN-CR-020 D020-01）。
 * <p>
 * 启动期构建单例目录并做 fail-fast 校验：空目录、空版本、重复/非法编码、展示名为空
 * 一律抛 {@link IllegalStateException} 使应用启动失败，不带空目录对外服务。
 * 日志只记录 catalogVersion 与条目数量，不记录完整目录（避免日志成为第二份配置来源）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PrivacyAuthorizationCatalogConfiguration {

    private final TspUserVehicleProperties properties;

    /**
     * 隐私授权项目录单例（配置即权威）
     *
     * @return 不可变目录
     */
    @Bean
    public PrivacyAuthorizationCatalog privacyAuthorizationCatalog() {
        TspUserVehicleProperties.PrivacyAuthorization config = properties.getPrivacyAuthorization();
        List<PrivacyAuthorizationCatalogPolicy.RawItem> rawItems = config.getItems().stream()
                .map(item -> new PrivacyAuthorizationCatalogPolicy.RawItem(item.getItem(), item.getDisplayName(),
                        item.getDisplayNameEn(), item.isRequired()))
                .toList();
        PrivacyAuthorizationCatalog catalog = PrivacyAuthorizationCatalogPolicy.build(
                config.getCatalogVersion(), rawItems);
        log.info("隐私授权项目录加载完成: catalogVersion={}, itemCount={}, enabled={}, enforceCatalog={}",
                catalog.catalogVersion(), catalog.items().size(), config.isEnabled(), config.isEnforceCatalog());
        return catalog;
    }
}
