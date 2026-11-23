package net.hwyz.iov.cloud.iov.tsp.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 车端只读获取批准隐私授权项目录（IOV-TSP-DSN-CR-020 D020-02，G-1 定稿）。
 * <p>
 * 契约形状（不可改名）：{@code { catalogVersion, items[{authorizationItem, displayName, required}] }}，
 * 响应条目<b>不含 currentVersion</b>。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacyAuthorizationItemsVo {

    /**
     * 目录版本（任何目录变更都必须变更该值；上报必须原样回传，否则 401119）
     */
    private String catalogVersion;

    /**
     * 批准授权项条目（顺序即服务端配置顺序，不含 currentVersion）
     */
    private List<PrivacyAuthorizationItemVo> items;
}
