package net.hwyz.iov.cloud.iov.tsp.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批准隐私授权项目录条目（IOV-TSP-DSN-CR-020 D020-02，G-1 定稿）。
 * <p>
 * G-1 定稿字段集：{@code authorizationItem + displayName + required}；
 * <b>不再承载 currentVersion</b>——授权项版本只由目录级 {@code catalogVersion} 承载，
 * 上报 {@code items[].version} 是客户端决策序号，两者语义不同不可混用。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacyAuthorizationItemVo {

    /**
     * 授权项编码（^[A-Z][A-Z0-9_]{2,63}$，区分大小写）
     */
    private String authorizationItem;

    /**
     * 展示名称（按 Accept-Language 返回，缺省/不支持语言回落 zh-CN）
     */
    private String displayName;

    /**
     * 是否必选授权项（关闭/拒绝不等于授权）
     */
    private Boolean required;
}
