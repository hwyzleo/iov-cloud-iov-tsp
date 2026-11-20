package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AgreementBundle;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AgreementContent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.AgreementContentPort;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.DependencyUnavailableException;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import net.hwyz.iov.cloud.mkt.cms.api.service.AgreementContentService;
import net.hwyz.iov.cloud.mkt.cms.api.vo.response.AgreementBundleResponse;
import net.hwyz.iov.cloud.mkt.cms.api.vo.response.AgreementContentResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MKT-CMS 协议内容防腐适配（IOV-TSP-DSN-CR-017 D017-01）。
 * <p>
 * 只读取已发布且不可变的修订；内容不可用时抛依赖不可用异常，不伪造协议、不使用可变 URL 冒充同意证据。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CmsAgreementContentAdapter implements AgreementContentPort {

    private final AgreementContentService agreementContentService;
    private final TspUserVehicleProperties properties;

    @Override
    public AgreementBundle loadCurrent(String language) {
        AgreementBundleResponse bundle;
        try {
            bundle = agreementContentService.getCurrentBundle(properties.getAgreementScene(),
                    null, language, null, null);
        } catch (Exception e) {
            throw new DependencyUnavailableException("CMS 协议内容不可用", e);
        }
        if (bundle == null || bundle.getAgreements() == null || bundle.getAgreements().isEmpty()) {
            throw new DependencyUnavailableException("CMS 未返回可用协议内容");
        }
        List<AgreementContent> contents = new ArrayList<>();
        for (AgreementContentResponse agreement : bundle.getAgreements()) {
            if (agreement == null) {
                continue;
            }
            contents.add(new AgreementContent(agreement.getAgreementType(), agreement.getVersion(),
                    agreement.getRevisionId(), agreement.getLocale(), agreement.getTitle(),
                    agreement.getContentHash(), agreement.getBody()));
        }
        return new AgreementBundle(bundle.getBundleId(), bundle.getLocale(), contents);
    }
}
