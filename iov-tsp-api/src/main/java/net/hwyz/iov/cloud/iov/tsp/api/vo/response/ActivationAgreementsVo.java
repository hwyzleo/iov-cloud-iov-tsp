package net.hwyz.iov.cloud.iov.tsp.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 激活协议查询结果（隐私政策 + 用户协议）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationAgreementsVo {

    /**
     * 发布包标识
     */
    private String bundleId;

    /**
     * 语言
     */
    private String language;

    /**
     * 两项协议（按 agreementType 识别，不依赖顺序）
     */
    private List<ActivationAgreementVo> agreements;
}
