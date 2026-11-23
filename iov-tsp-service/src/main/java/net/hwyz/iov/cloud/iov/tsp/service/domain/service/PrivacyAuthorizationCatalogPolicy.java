package net.hwyz.iov.cloud.iov.tsp.service.domain.service;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.PrivacyAuthorizationCatalog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.PrivacyAuthorizationItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 隐私授权项目录策略（IOV-TSP-DSN-CR-020 D020-01，G-1 定稿）。
 * <p>
 * 不可变、无 I/O、无 Spring 行为依赖（风格对齐 CIAM {@code DeviceScopePolicy}）：
 * <ul>
 *   <li>{@link #build} 为启动期构建，任何配置错误抛 {@link IllegalStateException} 使应用启动失败
 *       （fail-fast：不带空目录/非法目录对外服务）；</li>
 *   <li>{@link #unknownItems} 为上报期规则 2：返回目录外编码列表，不校验 version、不改写入参。</li>
 * </ul>
 * 「目录内容即权威」：授权项属产品配置，不落库、不使用 CMS，变更靠 catalogVersion 追溯。
 *
 * @author hwyz_leo
 */
public final class PrivacyAuthorizationCatalogPolicy {

    /**
     * 授权项编码规范（与既有命名风格一致，区分大小写，比较前 trim，不做大小写折叠）
     */
    public static final Pattern ITEM_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{2,63}$");

    private PrivacyAuthorizationCatalogPolicy() {
    }

    /**
     * 启动期构建目录：空目录/空版本/重复编码/编码格式非法/展示名为空 → IllegalStateException（启动失败）
     *
     * @param catalogVersion 目录版本
     * @param rawItems       原始条目配置
     * @return 不可变目录
     */
    public static PrivacyAuthorizationCatalog build(String catalogVersion, List<RawItem> rawItems) {
        if (catalogVersion == null || catalogVersion.isBlank()) {
            throw new IllegalStateException("隐私授权项目录版本未配置（tsp.user-vehicle.privacy-authorization.catalog-version）");
        }
        if (rawItems == null || rawItems.isEmpty()) {
            throw new IllegalStateException("隐私授权项目录为空（tsp.user-vehicle.privacy-authorization.items 必须至少一项）");
        }
        String version = catalogVersion.trim();
        List<PrivacyAuthorizationItem> items = new ArrayList<>(rawItems.size());
        Map<String, Map<String, String>> localizedNames = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (RawItem raw : rawItems) {
            if (raw == null || raw.item() == null || raw.item().isBlank()) {
                throw new IllegalStateException("隐私授权项编码不能为空（catalogVersion=" + version + "）");
            }
            String code = raw.item().trim();
            if (!ITEM_CODE_PATTERN.matcher(code).matches()) {
                throw new IllegalStateException("隐私授权项编码格式非法: " + code
                        + "（须匹配 ^[A-Z][A-Z0-9_]{2,63}$）");
            }
            if (!seen.add(code)) {
                throw new IllegalStateException("隐私授权项编码重复: " + code);
            }
            if (raw.displayName() == null || raw.displayName().isBlank()) {
                throw new IllegalStateException("隐私授权项展示名不能为空: " + code);
            }
            items.add(new PrivacyAuthorizationItem(code, raw.displayName().trim(), raw.required()));
            if (raw.displayNameEn() != null && !raw.displayNameEn().isBlank()) {
                localizedNames
                        .computeIfAbsent(PrivacyAuthorizationCatalog.ENGLISH_LANGUAGE, key -> new LinkedHashMap<>())
                        .put(code, raw.displayNameEn().trim());
            }
        }
        return new PrivacyAuthorizationCatalog(version, items, localizedNames);
    }

    /**
     * G-1 规则 2：返回目录外（未知）编码列表；不校验 version、不改写入参
     *
     * @param catalog 当前目录
     * @param items   上报授权项编码（调用方已 trim）
     * @return 未知编码列表（保持上报顺序，可能重复）
     */
    public static List<String> unknownItems(PrivacyAuthorizationCatalog catalog, List<String> items) {
        if (catalog == null || items == null || items.isEmpty()) {
            return List.of();
        }
        List<String> unknown = new ArrayList<>();
        for (String item : items) {
            if (item == null || catalog.find(item.trim()).isEmpty()) {
                unknown.add(item == null ? "" : item);
            }
        }
        return unknown;
    }

    /**
     * 原始目录条目配置（配置装配层 → 策略层的不可变输入）
     *
     * @param item          授权项编码
     * @param displayName   默认语言（zh-CN）展示名
     * @param displayNameEn 英文（en-US）展示名，可空
     * @param required      是否必选授权项
     */
    public record RawItem(String item, String displayName, String displayNameEn, boolean required) {
    }
}
