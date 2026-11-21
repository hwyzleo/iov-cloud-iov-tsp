package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.external;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AgreementBundle;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.DependencyUnavailableException;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import net.hwyz.iov.cloud.mkt.cms.api.service.AgreementContentService;
import net.hwyz.iov.cloud.mkt.cms.api.vo.response.AgreementBundleResponse;
import net.hwyz.iov.cloud.mkt.cms.api.vo.response.AgreementContentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * CMS 协议内容防腐层契约对齐测试（MKT-CMS-DSN-CR-001 / IOV-TSP-DSN-CR-017）。
 */
@ExtendWith(MockitoExtension.class)
class CmsAgreementContentAdapterTest {

    @Mock
    private AgreementContentService agreementContentService;

    private CmsAgreementContentAdapter adapter() {
        return new CmsAgreementContentAdapter(agreementContentService, new TspUserVehicleProperties());
    }

    @Test
    @DisplayName("按 IVI_ACTIVATION 场景读取两项协议并映射不可变修订证据")
    void mapsBundle() {
        when(agreementContentService.getCurrentBundle(eq("IVI_ACTIVATION"), any(), eq("zh-CN"), any(), any()))
                .thenReturn(AgreementBundleResponse.builder()
                        .bundleId("B-1").locale("zh-CN").publishedAt(Instant.parse("2026-11-20T00:00:00Z"))
                        .agreements(List.of(
                                AgreementContentResponse.builder().agreementType("PRIVACY_POLICY")
                                        .version("1.0").revisionId("REV-P").locale("zh-CN").title("隐私政策")
                                        .contentHash("sha256:p").body("<p/>").build(),
                                AgreementContentResponse.builder().agreementType("USER_AGREEMENT")
                                        .version("1.0").revisionId("REV-U").locale("zh-CN").title("用户协议")
                                        .contentHash("sha256:u").body("<p/>").build()))
                        .build());

        AgreementBundle bundle = adapter().loadCurrent("zh-CN");

        assertThat(bundle.bundleId()).isEqualTo("B-1");
        assertThat(bundle.language()).isEqualTo("zh-CN");
        assertThat(bundle.find("PRIVACY_POLICY").revisionId()).isEqualTo("REV-P");
        assertThat(bundle.find("USER_AGREEMENT").contentHash()).isEqualTo("sha256:u");
    }

    @Test
    @DisplayName("CMS 内容缺失或异常时按依赖不可用拒绝，不伪造协议")
    void dependencyFailures() {
        when(agreementContentService.getCurrentBundle(any(), any(), any(), any(), any()))
                .thenReturn(AgreementBundleResponse.builder().bundleId("B-1").agreements(List.of()).build());
        assertThatThrownBy(() -> adapter().loadCurrent("zh-CN"))
                .isInstanceOf(DependencyUnavailableException.class);

        when(agreementContentService.getCurrentBundle(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("cms down"));
        assertThatThrownBy(() -> adapter().loadCurrent("zh-CN"))
                .isInstanceOf(DependencyUnavailableException.class);
    }
}
