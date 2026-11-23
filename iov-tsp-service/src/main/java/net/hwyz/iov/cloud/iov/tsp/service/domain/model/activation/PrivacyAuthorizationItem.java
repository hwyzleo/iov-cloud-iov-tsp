package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

/**
 * 批准的隐私授权项目录条目（IOV-TSP-DSN-CR-020 D020-01，G-1 定稿）。
 * <p>
 * 不可变值对象，无 I/O、无 Spring 依赖：只承载「授权项编码 + 展示名 + 是否必选」。
 * <b>不承载 currentVersion</b>：授权项没有独立版本号，版本只由目录级 catalogVersion 承载；
 * 上报 {@code items[].version} 是客户端决策序号，与本值对象无关。
 *
 * @param authorizationItem 授权项编码（^[A-Z][A-Z0-9_]{2,63}$，区分大小写）
 * @param displayName       展示名称（已按请求语言本地化）
 * @param required          是否必选授权项
 * @author hwyz_leo
 */
public record PrivacyAuthorizationItem(String authorizationItem, String displayName, boolean required) {
}
