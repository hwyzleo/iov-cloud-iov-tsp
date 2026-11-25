package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ActivationSessionState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CredentialRevocationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.FirstActivationState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.IviLoginResultStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.MptUserVehicleDetailStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UnbindRelationState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UserVehicleBindingStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.MptUserVehicleHistoryRequest;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUnbindOperationResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUserVehicleHistoryResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUserVehicleRelationResult;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.UserVehicleBindingErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationSession;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.BindingOperation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.FirstActivationFact;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.MptUserVehicleHistoryCriteria;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.MptUserVehicleHistoryItem;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleRelation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleUnbindOperation;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.DependencyUnavailableException;
import net.hwyz.iov.cloud.iov.tsp.service.domain.port.FirstActivationQueryPort;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationSessionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.BindingOperationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.MptUserVehicleQueryRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleRelationRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.UserVehicleUnbindOperationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * MPT 用户车辆关系查询测试（IOV-TSP-DSN-CR-021 TC021-01～TC021-03、TC021-09）。
 * <p>
 * 覆盖 BOUND/UNBOUND/最近释放关系、登录与激活独立、VMD 故障 PARTIAL/UNKNOWN、
 * 不以 Outbox 推导、历史过滤与物理分页入口，以及操作查询语义。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MptUserVehicleQueryAppServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-25T10:00:00Z");
    private static final String VIN = "LSVAA1234567890AB";
    private static final String ACCOUNT = "account-1";
    private static final String RELATION_ID = "REL-1";
    private static final String SESSION_ID = "SES-1";
    private static final String OPERATION_ID = "BOP-1";
    private static final String UNBIND_OPERATION_ID = "UBO-1";

    @Mock
    private UserVehicleRelationRepository userVehicleRelationRepository;
    @Mock
    private UserVehicleUnbindOperationRepository userVehicleUnbindOperationRepository;
    @Mock
    private BindingOperationRepository bindingOperationRepository;
    @Mock
    private ActivationSessionRepository activationSessionRepository;
    @Mock
    private MptUserVehicleQueryRepository mptUserVehicleQueryRepository;
    @Mock
    private FirstActivationQueryPort firstActivationQueryPort;

    private MptUserVehicleQueryAppService appService;

    @BeforeEach
    void setUp() {
        appService = new MptUserVehicleQueryAppService(userVehicleRelationRepository,
                userVehicleUnbindOperationRepository, bindingOperationRepository, activationSessionRepository,
                mptUserVehicleQueryRepository, firstActivationQueryPort);
    }

    private UserVehicleRelation relation(String state) {
        return UserVehicleRelation.builder()
                .relationId(RELATION_ID).accountId(ACCOUNT).vin(VIN).role("PRIMARY_USER").state(state)
                .boundAt(NOW.minusSeconds(7200)).sourceOperationId(OPERATION_ID)
                .releasedAt("RELEASED".equals(state) ? NOW.minusSeconds(60) : null)
                .releaseReason("RELEASED".equals(state) ? "CUSTOMER_VERIFIED_REQUEST" : null)
                .releaseOperationId("RELEASED".equals(state) ? UNBIND_OPERATION_ID : null)
                .releasedBy("RELEASED".equals(state) ? "mpt-admin" : null)
                .rowVersion(2)
                .build();
    }

    @Test
    @DisplayName("BOUND：三事实独立读取，登录成功不推导首次激活")
    void boundRelationKeepsFactsIndependent() {
        when(userVehicleRelationRepository.findActiveByVin(VIN)).thenReturn(Optional.of(relation("ACTIVE")));
        when(bindingOperationRepository.findByOperationId(OPERATION_ID))
                .thenReturn(Optional.of(BindingOperation.builder().operationId(OPERATION_ID)
                        .sessionId(SESSION_ID).resultRelationId(RELATION_ID).build()));
        when(activationSessionRepository.findBySessionId(SESSION_ID))
                .thenReturn(Optional.of(ActivationSession.builder().sessionId(SESSION_ID).vin(VIN)
                        .state(ActivationSessionState.BOUND).iviLoginState(IviLoginResultStatus.SUCCEEDED)
                        .iviLoginReportedAt(NOW.minusSeconds(30)).build()));
        when(firstActivationQueryPort.load(VIN))
                .thenReturn(new FirstActivationFact(FirstActivationState.NOT_RECORDED, null, null));

        MptUserVehicleRelationResult result = appService.getRelation(VIN);

        assertThat(result.getBindingStatus()).isEqualTo(UserVehicleBindingStatus.BOUND);
        assertThat(result.getDetailStatus()).isEqualTo(MptUserVehicleDetailStatus.COMPLETE);
        assertThat(result.getRelation().getRelationId()).isEqualTo(RELATION_ID);
        assertThat(result.getIviLogin().getState()).isEqualTo(IviLoginResultStatus.SUCCEEDED);
        assertThat(result.getFirstActivation().getState()).isEqualTo(FirstActivationState.NOT_RECORDED);
        assertThat(result.getLatestUnbind()).isNull();
        verify(userVehicleRelationRepository, never()).findLatestReleasedByVin(anyString());
    }

    @Test
    @DisplayName("UNBOUND：返回最近已释放关系与撤销状态，不作为当前授权")
    void unboundReturnsLatestReleasedRelation() {
        UserVehicleRelation released = relation("RELEASED");
        when(userVehicleRelationRepository.findActiveByVin(VIN)).thenReturn(Optional.empty());
        when(userVehicleRelationRepository.findLatestReleasedByVin(VIN)).thenReturn(Optional.of(released));
        when(bindingOperationRepository.findByOperationId(OPERATION_ID))
                .thenReturn(Optional.of(BindingOperation.builder().operationId(OPERATION_ID)
                        .sessionId(SESSION_ID).resultRelationId(RELATION_ID).build()));
        when(activationSessionRepository.findBySessionId(SESSION_ID))
                .thenReturn(Optional.of(ActivationSession.builder().sessionId(SESSION_ID).vin(VIN)
                        .state(ActivationSessionState.BOUND).iviLoginState(IviLoginResultStatus.FAILED)
                        .iviLoginReportedAt(NOW.minusSeconds(300)).build()));
        when(userVehicleUnbindOperationRepository.findByOperationId(UNBIND_OPERATION_ID))
                .thenReturn(Optional.of(UserVehicleUnbindOperation.builder()
                        .operationId(UNBIND_OPERATION_ID).relationId(RELATION_ID).vin(VIN)
                        .relationState(UnbindRelationState.RELEASED)
                        .credentialRevocationStatus(CredentialRevocationStatus.FAILED)
                        .releasedAt(NOW.minusSeconds(60)).reason("CUSTOMER_VERIFIED_REQUEST")
                        .build()));
        when(firstActivationQueryPort.load(VIN))
                .thenReturn(new FirstActivationFact(FirstActivationState.RECORDED, NOW.minusSeconds(3600), "ref"));

        MptUserVehicleRelationResult result = appService.getRelation(VIN);

        assertThat(result.getBindingStatus()).isEqualTo(UserVehicleBindingStatus.UNBOUND);
        assertThat(result.getRelation()).isNull();
        assertThat(result.getLatestReleasedRelation().getRelationId()).isEqualTo(RELATION_ID);
        assertThat(result.getLatestUnbind().getCredentialRevocationStatus())
                .isEqualTo(CredentialRevocationStatus.FAILED);
        assertThat(result.getIviLogin().getState()).isEqualTo(IviLoginResultStatus.FAILED);
        assertThat(result.getFirstActivation().getState()).isEqualTo(FirstActivationState.RECORDED);
    }

    @Test
    @DisplayName("VMD 不可用：firstActivation=UNKNOWN + detailStatus=PARTIAL，核心关系仍可用")
    void vmdFailureMapsPartialUnknown() {
        when(userVehicleRelationRepository.findActiveByVin(VIN)).thenReturn(Optional.of(relation("ACTIVE")));
        when(firstActivationQueryPort.load(VIN)).thenThrow(new DependencyUnavailableException("timeout"));

        MptUserVehicleRelationResult result = appService.getRelation(VIN);

        assertThat(result.getDetailStatus()).isEqualTo(MptUserVehicleDetailStatus.PARTIAL);
        assertThat(result.getFirstActivation().getState()).isEqualTo(FirstActivationState.UNKNOWN);
        assertThat(result.getRelation().getRelationId()).isEqualTo(RELATION_ID);
    }

    @Test
    @DisplayName("非法 VIN：401112 且不查询数据库/外部服务")
    void invalidVinRejected() {
        assertThatThrownBy(() -> appService.getRelation("bad-vin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.INVALID_REQUEST.getCode()));
        verifyNoInteractions(userVehicleRelationRepository, firstActivationQueryPort);
    }

    @Test
    @DisplayName("解绑操作查询：不存在按 401112，存在返回权威状态")
    void queryOperation() {
        assertThatThrownBy(() -> appService.queryOperation("missing"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.INVALID_REQUEST.getCode()));

        when(userVehicleUnbindOperationRepository.findByOperationId(UNBIND_OPERATION_ID))
                .thenReturn(Optional.of(UserVehicleUnbindOperation.builder()
                        .operationId(UNBIND_OPERATION_ID).relationId(RELATION_ID).vin(VIN)
                        .relationState(UnbindRelationState.RELEASED)
                        .credentialRevocationStatus(CredentialRevocationStatus.PENDING)
                        .retryCount(2).nextRetryAt(NOW.plusSeconds(30)).rowVersion(2).build()));

        MptUnbindOperationResult result = appService.queryOperation(UNBIND_OPERATION_ID);

        assertThat(result.getRelationState()).isEqualTo(UnbindRelationState.RELEASED);
        assertThat(result.getCredentialRevocationStatus()).isEqualTo(CredentialRevocationStatus.PENDING);
        assertThat(result.getRetryCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("历史查询：条件在数据库侧过滤，结果映射撤销状态")
    void searchHistoryMapsItems() {
        when(mptUserVehicleQueryRepository.searchHistory(any())).thenReturn(List.of(
                new MptUserVehicleHistoryItem(RELATION_ID, VIN, ACCOUNT, "PRIMARY_USER", "RELEASED",
                        NOW.minusSeconds(7200), NOW.minusSeconds(60), "CUSTOMER_VERIFIED_REQUEST",
                        UNBIND_OPERATION_ID, "mpt-admin", CredentialRevocationStatus.SUCCEEDED)));

        PageResult<MptUserVehicleHistoryResult> page = appService.searchHistory(
                MptUserVehicleHistoryRequest.builder().vin(VIN).relationId(" ").operationId(UNBIND_OPERATION_ID)
                        .pageNo(1).pageSize(20).build());

        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getItems().get(0).getCredentialRevocationStatus())
                .isEqualTo(CredentialRevocationStatus.SUCCEEDED);
        verify(mptUserVehicleQueryRepository).searchHistory(
                new MptUserVehicleHistoryCriteria(VIN, null, UNBIND_OPERATION_ID));
    }

    @Test
    @DisplayName("历史查询：非法 VIN 拒绝，不查数据库")
    void searchHistoryRejectsInvalidVin() {
        assertThatThrownBy(() -> appService.searchHistory(
                MptUserVehicleHistoryRequest.builder().vin("bad").build()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode().getCode())
                        .isEqualTo(UserVehicleBindingErrorCode.INVALID_REQUEST.getCode()));
        verify(mptUserVehicleQueryRepository, never()).searchHistory(any());
    }
}
