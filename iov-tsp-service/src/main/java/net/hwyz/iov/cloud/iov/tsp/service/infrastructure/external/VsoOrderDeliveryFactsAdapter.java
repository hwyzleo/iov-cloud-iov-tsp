package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.otd.vso.api.enums.BindingDeliveryStatus;
import net.hwyz.iov.cloud.otd.vso.api.enums.BindingLookupStatus;
import net.hwyz.iov.cloud.otd.vso.api.service.VsoOrderService;
import net.hwyz.iov.cloud.otd.vso.api.vo.request.BindingFactsQueryRequest;
import net.hwyz.iov.cloud.otd.vso.api.vo.response.BindingFactsQueryResponse;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.DeliveryFacts;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.DeliveryFactsQuality;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.DependencyUnavailableException;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.OrderDeliveryFactsPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * OTD-VSO 绑车事实防腐适配（IOV-TSP-DSN-CR-017 D017-03）。
 * <p>
 * VSO 只提供最小事实与核验状态，不提供资格结论；本适配把外部枚举归一化为
 * TSP 内部事实质量，并把技术失败与「无匹配」严格区分（技术失败不得当作资格不符）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VsoOrderDeliveryFactsAdapter implements OrderDeliveryFactsPort {

    private static final int FACT_REF_MAX_LENGTH = 512;

    private final VsoOrderService vsoOrderService;

    @Override
    public DeliveryFacts load(String accountId, String vin) {
        BindingFactsQueryResponse response;
        try {
            response = vsoOrderService.queryBindingFacts(BindingFactsQueryRequest.builder()
                    .accountId(accountId)
                    .vin(vin)
                    .build());
        } catch (Exception e) {
            throw new DependencyUnavailableException("VSO 绑车事实查询不可用: vin=" + mask(vin), e);
        }
        if (response == null || response.getLookupStatus() == null) {
            // 无响应不等于「无订单」，不得降级为资格不符
            throw new DependencyUnavailableException("VSO 绑车事实响应缺失: vin=" + mask(vin));
        }
        DeliveryFactsQuality quality = mapQuality(response.getLookupStatus());
        if (quality != DeliveryFactsQuality.MATCHED || response.getFacts() == null || response.getFacts().isEmpty()) {
            // 非 MATCHED 也保留查询结论与原因码，便于审计与事后复核（不含 PII）
            return new DeliveryFacts(quality, false, List.of(), null,
                    buildLookupRef(response, quality), response.getCheckedAt());
        }
        BindingFactsQueryResponse.Fact fact = response.getFacts().get(0);
        boolean delivered = fact.getDeliveryStatus() == BindingDeliveryStatus.DELIVERED;
        return new DeliveryFacts(quality, delivered,
                fact.getMatchedRoles() == null ? List.of() : fact.getMatchedRoles(),
                fact.getOrderNo(), buildFactRef(fact, response), response.getCheckedAt());
    }

    private DeliveryFactsQuality mapQuality(BindingLookupStatus status) {
        return switch (status) {
            case MATCHED -> DeliveryFactsQuality.MATCHED;
            case NO_MATCH -> DeliveryFactsQuality.NO_MATCH;
            case AMBIGUOUS -> DeliveryFactsQuality.AMBIGUOUS;
            case INSUFFICIENT -> DeliveryFactsQuality.INSUFFICIENT;
            case INCONSISTENT -> DeliveryFactsQuality.INCONSISTENT;
        };
    }

    /**
     * 非 MATCHED 结论的引用摘要（只含核验状态与原因码，不含 PII）
     */
    private String buildLookupRef(BindingFactsQueryResponse response, DeliveryFactsQuality quality) {
        String reasonCode = response.getReasonCode() == null ? "-" : response.getReasonCode().toString();
        return "lookup=" + quality.name() + ";reason=" + reasonCode;
    }

    /**
     * 事实引用只保留业务标识与真实版本，不复制订单快照或客户 PII
     */
    private String buildFactRef(BindingFactsQueryResponse.Fact fact, BindingFactsQueryResponse response) {
        List<String> refs = new ArrayList<>();
        refs.add("schemaVersion=1");
        if (response.getReasonCode() != null) {
            refs.add("reason=" + response.getReasonCode());
        }
        if (fact.getOrderNo() != null) {
            refs.add("order=" + fact.getOrderNo());
        }
        if (fact.getEvidenceRefs() != null) {
            for (BindingFactsQueryResponse.EvidenceRef ref : fact.getEvidenceRefs()) {
                if (ref == null) {
                    continue;
                }
                refs.add(ref.getType() + ":" + ref.getId() + "#" + ref.getVersion());
            }
        }
        String joined = String.join(";", refs);
        return joined.length() > FACT_REF_MAX_LENGTH ? joined.substring(0, FACT_REF_MAX_LENGTH) : joined;
    }

    private String mask(String vin) {
        if (vin == null || vin.length() <= 7) {
            return "***";
        }
        return vin.substring(0, 3) + "***" + vin.substring(vin.length() - 4);
    }
}
