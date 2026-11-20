package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

/**
 * 单项已发布协议内容（来源 MKT-CMS，含不可变修订证据）。
 *
 * @author hwyz_leo
 */
public record AgreementContent(String agreementType,
                               String version,
                               String revisionId,
                               String language,
                               String title,
                               String contentHash,
                               String content) {
}
