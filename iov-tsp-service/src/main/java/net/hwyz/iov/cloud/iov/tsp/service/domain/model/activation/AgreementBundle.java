package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

import java.util.List;

/**
 * 当前发布包（固定两项协议）。
 *
 * @author hwyz_leo
 */
public record AgreementBundle(String bundleId, String language, List<AgreementContent> agreements) {

    /**
     * 按类型查找协议
     *
     * @param agreementType 协议类型
     * @return 协议内容；不存在返回 null
     */
    public AgreementContent find(String agreementType) {
        if (agreements == null) {
            return null;
        }
        return agreements.stream()
                .filter(item -> item.agreementType() != null && item.agreementType().equals(agreementType))
                .findFirst()
                .orElse(null);
    }
}
