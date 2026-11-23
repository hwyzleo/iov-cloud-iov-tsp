package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.external;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.DeliveryFacts;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.DeliveryFactsQuality;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.DependencyUnavailableException;
import net.hwyz.iov.cloud.otd.vso.api.enums.BindingDeliveryStatus;
import net.hwyz.iov.cloud.otd.vso.api.enums.BindingEvidenceType;
import net.hwyz.iov.cloud.otd.vso.api.enums.BindingLookupStatus;
import net.hwyz.iov.cloud.otd.vso.api.enums.BindingReasonCode;
import net.hwyz.iov.cloud.otd.vso.api.service.VsoOrderService;
import net.hwyz.iov.cloud.otd.vso.api.vo.response.BindingFactsQueryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * VSO 绑车事实防腐层契约对齐测试（OTD-VSO-DSN-CR-017 / IOV-TSP-DSN-CR-017、IOV-TSP-DSN-CR-020 TC020-05）。
 */
@ExtendWith(MockitoExtension.class)
class VsoOrderDeliveryFactsAdapterTest {

    private static final Instant CHECKED_AT = Instant.parse("2026-11-20T10:00:00Z");

    @Mock
    private VsoOrderService vsoOrderService;

    @InjectMocks
    private VsoOrderDeliveryFactsAdapter adapter;

    @Test
    @DisplayName("MATCHED + DELIVERED：归一为已交付并生成证据引用")
    void matchedDelivered() {
        when(vsoOrderService.queryBindingFacts(any())).thenReturn(BindingFactsQueryResponse.builder()
                .schemaVersion(1).checkedAt(CHECKED_AT).lookupStatus(BindingLookupStatus.MATCHED)
                .facts(List.of(BindingFactsQueryResponse.Fact.builder()
                        .orderId("OID-1").orderNo("ORDER-1").orderState(650)
                        .vin("LSVAA1234567890AB").matchedRoles(List.of("order_user"))
                        .deliveryStatus(BindingDeliveryStatus.DELIVERED).deliveryRecordId("DR-1")
                        .actualDeliveryTime(CHECKED_AT)
                        .evidenceRefs(List.of(BindingFactsQueryResponse.EvidenceRef.builder()
                                .type(BindingEvidenceType.ORDER).id("OID-1").version(3).build()))
                        .build()))
                .build());

        DeliveryFacts facts = adapter.load("account-1", "LSVAA1234567890AB");

        assertThat(facts.quality()).isEqualTo(DeliveryFactsQuality.MATCHED);
        assertThat(facts.delivered()).isTrue();
        assertThat(facts.rolesPresent()).isTrue();
        assertThat(facts.factRef()).contains("order=ORDER-1").contains("ORDER:OID-1#3");
        assertThat(facts.checkedAt()).isEqualTo(CHECKED_AT);
    }

    @Test
    @DisplayName("NO_MATCH：保留结论与原因码，不丢弃追溯信息")
    void noMatchKeepsReason() {
        when(vsoOrderService.queryBindingFacts(any())).thenReturn(BindingFactsQueryResponse.builder()
                .schemaVersion(1).checkedAt(CHECKED_AT).lookupStatus(BindingLookupStatus.NO_MATCH)
                .reasonCode(BindingReasonCode.NO_ACCOUNT_VIN_MATCH).facts(List.of()).build());

        DeliveryFacts facts = adapter.load("account-1", "LSVAA1234567890AB");

        assertThat(facts.quality()).isEqualTo(DeliveryFactsQuality.NO_MATCH);
        assertThat(facts.delivered()).isFalse();
        assertThat(facts.factRef()).contains("lookup=NO_MATCH").contains("reason=NO_ACCOUNT_VIN_MATCH");
    }

    @Test
    @DisplayName("AMBIGUOUS/INSUFFICIENT/INCONSISTENT 按事实质量归一，不当作资格不符")
    void undeterminedQualities() {
        for (var status : List.of(BindingLookupStatus.AMBIGUOUS, BindingLookupStatus.INSUFFICIENT,
                BindingLookupStatus.INCONSISTENT)) {
            when(vsoOrderService.queryBindingFacts(any())).thenReturn(BindingFactsQueryResponse.builder()
                    .schemaVersion(1).checkedAt(CHECKED_AT).lookupStatus(status).facts(List.of()).build());
            DeliveryFacts facts = adapter.load("account-1", "LSVAA1234567890AB");
            assertThat(facts.quality().name()).isEqualTo(status.name());
            assertThat(facts.delivered()).isFalse();
        }
    }

    @Test
    @DisplayName("MATCHED + 多条事实：归一为 AMBIGUOUS，不静默选单且不返回 orderNo")
    void matchedMultipleFactsIsAmbiguous() {
        when(vsoOrderService.queryBindingFacts(any())).thenReturn(BindingFactsQueryResponse.builder()
                .schemaVersion(1).checkedAt(CHECKED_AT).lookupStatus(BindingLookupStatus.MATCHED)
                .facts(List.of(fact("ORDER-1", BindingDeliveryStatus.DELIVERED),
                        fact("ORDER-2", BindingDeliveryStatus.DELIVERED),
                        fact("ORDER-3", BindingDeliveryStatus.NOT_DELIVERED)))
                .build());

        DeliveryFacts facts = adapter.load("account-1", "LSVAA1234567890AB");

        assertThat(facts.quality()).isEqualTo(DeliveryFactsQuality.AMBIGUOUS);
        assertThat(facts.delivered()).isFalse();
        assertThat(facts.orderNo()).isNull();
        assertThat(facts.matchedRoles()).isEmpty();
        assertThat(facts.factRef()).contains("lookup=AMBIGUOUS").contains("count=3")
                .doesNotContain("ORDER-");
    }

    @Test
    @DisplayName("MATCHED + 空事实：归一为 INSUFFICIENT（不降级为资格不符）")
    void matchedEmptyFactsIsInsufficient() {
        when(vsoOrderService.queryBindingFacts(any())).thenReturn(BindingFactsQueryResponse.builder()
                .schemaVersion(1).checkedAt(CHECKED_AT).lookupStatus(BindingLookupStatus.MATCHED)
                .facts(List.of()).build());

        DeliveryFacts facts = adapter.load("account-1", "LSVAA1234567890AB");

        assertThat(facts.quality()).isEqualTo(DeliveryFactsQuality.INSUFFICIENT);
        assertThat(facts.delivered()).isFalse();
        assertThat(facts.orderNo()).isNull();
        assertThat(facts.factRef()).contains("lookup=MATCHED").contains("facts=0");
    }

    private BindingFactsQueryResponse.Fact fact(String orderNo, BindingDeliveryStatus status) {
        return BindingFactsQueryResponse.Fact.builder()
                .orderId("OID-" + orderNo).orderNo(orderNo).orderState(650)
                .vin("LSVAA1234567890AB").matchedRoles(List.of("order_user"))
                .deliveryStatus(status).build();
    }

    @Test
    @DisplayName("空响应/异常按依赖不可用处理，不降级为无订单")
    void dependencyFailures() {
        when(vsoOrderService.queryBindingFacts(any())).thenReturn(null);
        assertThatThrownBy(() -> adapter.load("account-1", "LSVAA1234567890AB"))
                .isInstanceOf(DependencyUnavailableException.class);

        when(vsoOrderService.queryBindingFacts(any())).thenThrow(new RuntimeException("timeout"));
        assertThatThrownBy(() -> adapter.load("account-1", "LSVAA1234567890AB"))
                .isInstanceOf(DependencyUnavailableException.class);
    }
}
