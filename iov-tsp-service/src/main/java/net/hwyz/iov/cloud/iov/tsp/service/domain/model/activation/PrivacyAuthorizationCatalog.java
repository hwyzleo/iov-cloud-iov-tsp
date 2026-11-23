package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 隐私授权项目录（IOV-TSP-DSN-CR-020 D020-01，G-1 定稿）。
 * <p>
 * 不可变值对象，无 I/O、无 Spring 行为依赖：只承载「目录版本 + 授权项条目 + 多语言展示名」。
 * 目录是授权事实的唯一权威：上报的 {@code authorizationItem} 必须命中本目录，
 * {@code catalogVersion} 必须与本目录版本一致。
 *
 * @param catalogVersion        目录版本（任何条目变更必须同步变更该值）
 * @param items                 授权项条目（顺序即配置顺序，对外输出顺序稳定）
 * @param localizedDisplayNames 其它语言的展示名：语言标签 → （授权项编码 → 展示名）
 * @author hwyz_leo
 */
public record PrivacyAuthorizationCatalog(String catalogVersion,
                                          List<PrivacyAuthorizationItem> items,
                                          Map<String, Map<String, String>> localizedDisplayNames) {

    /**
     * 默认语言（Accept-Language 缺省或语言不支持时回落）
     */
    public static final String DEFAULT_LANGUAGE = "zh-CN";

    /**
     * 英文语言标签（本期唯一受支持的非默认语言）
     */
    public static final String ENGLISH_LANGUAGE = "en-US";

    public PrivacyAuthorizationCatalog {
        items = items == null ? List.of() : List.copyOf(items);
        localizedDisplayNames = localizedDisplayNames == null ? Map.of() : Map.copyOf(localizedDisplayNames);
    }

    /**
     * 不含多语言展示名的目录（仅默认展示名，用于测试与最小装配）
     *
     * @param catalogVersion 目录版本
     * @param items          授权项条目
     */
    public PrivacyAuthorizationCatalog(String catalogVersion, List<PrivacyAuthorizationItem> items) {
        this(catalogVersion, items, Map.of());
    }

    /**
     * 按授权项编码查找（区分大小写；调用方负责 trim）
     *
     * @param authorizationItem 授权项编码
     * @return 命中条目
     */
    public Optional<PrivacyAuthorizationItem> find(String authorizationItem) {
        if (authorizationItem == null) {
            return Optional.empty();
        }
        return items.stream()
                .filter(item -> authorizationItem.equals(item.authorizationItem()))
                .findFirst();
    }

    /**
     * G-1 规则 1：上报 catalogVersion 与服务端当前目录版本是否一致（不一致 → 401119）
     *
     * @param reportedCatalogVersion 上报的目录版本
     * @return 一致返回 true
     */
    public boolean versionMatches(String reportedCatalogVersion) {
        return catalogVersion != null && catalogVersion.equals(reportedCatalogVersion);
    }

    /**
     * R-20：按 Accept-Language 选择 displayName，缺省/不支持的语言回落 zh-CN（不报错、不做 406 协商）
     *
     * @param languageTag Accept-Language 头原始值（可含权重列表，如 "en-US,en;q=0.9"）
     * @return 本地化后的目录
     */
    public PrivacyAuthorizationCatalog localizedTo(String languageTag) {
        String language = resolveLanguage(languageTag);
        if (DEFAULT_LANGUAGE.equals(language)) {
            return this;
        }
        Map<String, String> names = localizedDisplayNames.getOrDefault(language, Map.of());
        if (names.isEmpty()) {
            return this;
        }
        List<PrivacyAuthorizationItem> localized = new ArrayList<>(items.size());
        for (PrivacyAuthorizationItem item : items) {
            String displayName = names.get(item.authorizationItem());
            localized.add(displayName == null || displayName.isBlank()
                    ? item
                    : new PrivacyAuthorizationItem(item.authorizationItem(), displayName, item.required()));
        }
        return new PrivacyAuthorizationCatalog(catalogVersion, localized, localizedDisplayNames);
    }

    /**
     * Accept-Language → 支持的目录语言（本期仅 zh-CN / en-US，其它一律回落 zh-CN）
     *
     * @param languageTag Accept-Language 头原始值
     * @return 支持的语言标签
     */
    public static String resolveLanguage(String languageTag) {
        if (languageTag == null || languageTag.isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        // 只取首个语言标签，忽略权重列表（不做 406 协商）
        String first = languageTag.split(",", 2)[0].trim();
        int semicolon = first.indexOf(';');
        if (semicolon >= 0) {
            first = first.substring(0, semicolon).trim();
        }
        if (first.isEmpty() || "*".equals(first)) {
            return DEFAULT_LANGUAGE;
        }
        String primary = first.replace('_', '-').toLowerCase(Locale.ROOT);
        int dash = primary.indexOf('-');
        if (dash >= 0) {
            primary = primary.substring(0, dash);
        }
        if ("en".equals(primary)) {
            return ENGLISH_LANGUAGE;
        }
        return DEFAULT_LANGUAGE;
    }

    /**
     * 多语言展示名视图（语言标签 → 授权项编码 → 展示名），构造期内部使用
     *
     * @return 可变的语言视图容器
     */
    public static Map<String, Map<String, String>> newDisplayNames() {
        return new LinkedHashMap<>();
    }
}
