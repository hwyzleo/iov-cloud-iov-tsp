package net.hwyz.iov.cloud.iov.tsp.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.AgreementType;

/**
 * 单项激活协议（来自 MKT-CMS 已发布修订）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationAgreementVo {

    /**
     * 协议类型
     */
    private AgreementType agreementType;

    /**
     * 业务展示版本
     */
    private String version;

    /**
     * 不可变修订标识
     */
    private String revisionId;

    /**
     * 语言
     */
    private String language;

    /**
     * 标题
     */
    private String title;

    /**
     * 正文摘要（同意证据引用）
     */
    private String contentHash;

    /**
     * 正文（安全 HTML 片段）
     */
    private String content;
}
