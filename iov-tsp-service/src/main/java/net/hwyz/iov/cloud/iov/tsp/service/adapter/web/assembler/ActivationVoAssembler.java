package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.AgreementType;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ActivationAgreementVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ActivationAgreementsVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.PrivacyAuthorizationItemVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.PrivacyAuthorizationItemsVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.UserVehicleRelationVo;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.UserVehicleRelationResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AgreementBundle;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AgreementContent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.PrivacyAuthorizationCatalog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.PrivacyAuthorizationItem;

import java.util.List;

/**
 * 用户车辆关系 VO 组装（协议内容来自 CMS，按 agreementType 识别，不依赖顺序）。
 *
 * @author hwyz_leo
 */
public final class ActivationVoAssembler {

    private ActivationVoAssembler() {
    }

    /**
     * 协议发布包 → VO
     *
     * @param bundle 协议发布包
     * @return 协议 VO
     */
    public static ActivationAgreementsVo toAgreementsVo(AgreementBundle bundle) {
        if (bundle == null) {
            return null;
        }
        List<ActivationAgreementVo> agreements = bundle.agreements() == null ? List.of()
                : bundle.agreements().stream().map(ActivationVoAssembler::toAgreementVo).toList();
        return ActivationAgreementsVo.builder()
                .bundleId(bundle.bundleId())
                .language(bundle.language())
                .agreements(agreements)
                .build();
    }

    /**
     * 当前账号人车关系应用结果 → 响应 VO（IOV-TSP-DSN-CR-018 D018-02）
     *
     * @param result 应用查询结果
     * @return 当前关系 VO
     */
    public static UserVehicleRelationVo toUserVehicleRelationVo(UserVehicleRelationResult result) {
        if (result == null) {
            return null;
        }
        return UserVehicleRelationVo.builder()
                .status(result.getStatus())
                .vin(result.getVin())
                .relationId(result.getRelationId())
                .role(result.getRole())
                .boundAt(result.getBoundAt())
                .build();
    }

    /**
     * 隐私授权项目录 → VO（IOV-TSP-DSN-CR-020 D020-02）
     * <p>
     * 字段集固定 {@code catalogVersion + items[{authorizationItem, displayName, required}]}，
     * <b>不含 currentVersion</b>；displayName 按 Accept-Language 选择（缺省/不支持语言回落 zh-CN）。
     *
     * @param catalog       服务端权威目录
     * @param acceptLanguage Accept-Language 头原始值（可空）
     * @return 目录 VO
     */
    public static PrivacyAuthorizationItemsVo toPrivacyAuthorizationItemsVo(PrivacyAuthorizationCatalog catalog,
                                                                            String acceptLanguage) {
        if (catalog == null) {
            return null;
        }
        PrivacyAuthorizationCatalog localized = catalog.localizedTo(acceptLanguage);
        List<PrivacyAuthorizationItemVo> items = localized.items() == null ? List.of()
                : localized.items().stream().map(ActivationVoAssembler::toPrivacyAuthorizationItemVo).toList();
        return PrivacyAuthorizationItemsVo.builder()
                .catalogVersion(localized.catalogVersion())
                .items(items)
                .build();
    }

    private static PrivacyAuthorizationItemVo toPrivacyAuthorizationItemVo(PrivacyAuthorizationItem item) {
        return PrivacyAuthorizationItemVo.builder()
                .authorizationItem(item.authorizationItem())
                .displayName(item.displayName())
                .required(item.required())
                .build();
    }

    private static ActivationAgreementVo toAgreementVo(AgreementContent content) {
        return ActivationAgreementVo.builder()
                .agreementType(parseType(content.agreementType()))
                .version(content.version())
                .revisionId(content.revisionId())
                .language(content.language())
                .title(content.title())
                .contentHash(content.contentHash())
                .content(content.content())
                .build();
    }

    private static AgreementType parseType(String type) {
        if (type == null) {
            return null;
        }
        try {
            return AgreementType.valueOf(type.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
