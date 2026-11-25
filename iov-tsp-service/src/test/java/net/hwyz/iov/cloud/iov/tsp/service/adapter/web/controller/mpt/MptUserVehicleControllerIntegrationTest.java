package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.mpt;

import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.framework.web.properties.IovRequestTimeZoneProperties;
import net.hwyz.iov.cloud.framework.web.timezone.AuthenticatedUserTimeZoneResolver;
import net.hwyz.iov.cloud.framework.web.timezone.DisplayTimeFacade;
import net.hwyz.iov.cloud.framework.web.timezone.RequestTimeZoneFallbackResolver;
import net.hwyz.iov.cloud.framework.web.timezone.RequestTimeZoneInterceptor;
import net.hwyz.iov.cloud.framework.web.timezone.RequestTimeZoneSnapshotResolver;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CredentialRevocationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.FirstActivationState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.MptUserVehicleDetailStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UnbindRelationState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UserVehicleBindingStatus;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUnbindOperationResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUserVehicleHistoryResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUserVehicleRelationResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUserVehicleUnbindResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.MptUserVehicleQueryAppService;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.UserVehicleUnbindAppService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MPT 用户车辆关系控制器集成测试（standalone MockMvc，IOV-TSP-DSN-CR-021）。
 * <p>
 * 覆盖五个端点路由、响应结构、账号脱敏与写操作可信操作人传递；
 * 权限注解与平台审计 AOP 由框架运行时装配，不在 standalone 中断言。
 */
class MptUserVehicleControllerIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-11-25T02:00:00Z");

    private MptUserVehicleQueryAppService queryAppService;
    private UserVehicleUnbindAppService unbindAppService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        queryAppService = mock(MptUserVehicleQueryAppService.class);
        unbindAppService = mock(UserVehicleUnbindAppService.class);
        IovRequestTimeZoneProperties tzProperties = new IovRequestTimeZoneProperties();
        ObjectProvider<AuthenticatedUserTimeZoneResolver> userProvider = mock(ObjectProvider.class);
        ObjectProvider<List<RequestTimeZoneFallbackResolver>> fallbackProvider = mock(ObjectProvider.class);
        RequestTimeZoneSnapshotResolver snapshotResolver =
                new RequestTimeZoneSnapshotResolver(tzProperties, userProvider, fallbackProvider);
        RequestTimeZoneInterceptor interceptor = new RequestTimeZoneInterceptor(snapshotResolver);
        DisplayTimeFacade facade = new DisplayTimeFacade(tzProperties, fallbackProvider);
        MptUserVehicleController controller =
                new MptUserVehicleController(queryAppService, unbindAppService, facade);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).addInterceptors(interceptor).build();
        SecurityContextHolder.setUserName("mpt-admin");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.getLocalMap().clear();
    }

    private MptUserVehicleRelationResult relationResult() {
        return MptUserVehicleRelationResult.builder()
                .vin("LSVAA1234567890AB")
                .bindingStatus(UserVehicleBindingStatus.BOUND)
                .detailStatus(MptUserVehicleDetailStatus.COMPLETE)
                .relation(MptUserVehicleRelationResult.RelationResult.builder()
                        .relationId("REL-1").accountId("account-01").role("PRIMARY_USER").state("ACTIVE")
                        .boundAt(NOW).rowVersion(3).build())
                .firstActivation(MptUserVehicleRelationResult.FirstActivationResult.builder()
                        .state(FirstActivationState.NOT_RECORDED).build())
                .build();
    }

    private MptUserVehicleUnbindResult unbindResult() {
        return MptUserVehicleUnbindResult.builder()
                .operationId("UBO-1").relationId("REL-1").vin("LSVAA1234567890AB")
                .relationState(UnbindRelationState.RELEASED)
                .credentialRevocationStatus(CredentialRevocationStatus.PENDING)
                .releasedAt(NOW)
                .build();
    }

    @Test
    @DisplayName("GET /relation：返回 BOUND 与脱敏账号，三事实独立")
    void relationRoute() throws Exception {
        when(queryAppService.getRelation("LSVAA1234567890AB")).thenReturn(relationResult());

        mockMvc.perform(get("/api/mpt/userVehicle/v1/relation")
                        .param("vin", "LSVAA1234567890AB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bindingStatus").value("BOUND"))
                .andExpect(jsonPath("$.data.detailStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.data.relation.relationId").value("REL-1"))
                .andExpect(jsonPath("$.data.relation.maskedAccountId").value("acc*****01"))
                .andExpect(jsonPath("$.data.relation.rowVersion").value(3))
                .andExpect(jsonPath("$.data.firstActivation.state").value("NOT_RECORDED"));
    }

    @Test
    @DisplayName("GET /relation/history：返回物理分页结果")
    void historyRoute() throws Exception {
        when(queryAppService.searchHistory(any())).thenReturn(PageResult.<MptUserVehicleHistoryResult>builder()
                .items(List.of(MptUserVehicleHistoryResult.builder()
                        .relationId("REL-1").vin("LSVAA1234567890AB").accountId("account-01")
                        .role("PRIMARY_USER").state("RELEASED").boundAt(NOW.minusSeconds(7200))
                        .releasedAt(NOW).releaseOperationId("UBO-1").releasedBy("mpt-admin")
                        .credentialRevocationStatus(CredentialRevocationStatus.SUCCEEDED).build()))
                .total(1).page(1).size(20).build());

        mockMvc.perform(get("/api/mpt/userVehicle/v1/relation/history")
                        .param("vin", "LSVAA1234567890AB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].maskedAccountId").value("acc*****01"))
                .andExpect(jsonPath("$.data.items[0].credentialRevocationStatus").value("SUCCEEDED"));
    }

    @Test
    @DisplayName("POST /relation/{id}/unbind：返回关系与撤销两个独立状态，操作人取可信上下文")
    void unbindRoute() throws Exception {
        when(unbindAppService.unbind(eq("mpt-admin"), eq("REL-1"), any())).thenReturn(unbindResult());

        mockMvc.perform(post("/api/mpt/userVehicle/v1/relation/REL-1/unbind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rowVersion\":3,\"reason\":\"CUSTOMER_VERIFIED_REQUEST\","
                                + "\"idempotencyKey\":\"mpt-unbind-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operationId").value("UBO-1"))
                .andExpect(jsonPath("$.data.relationState").value("RELEASED"))
                .andExpect(jsonPath("$.data.credentialRevocationStatus").value("PENDING"));
        verify(unbindAppService).unbind(eq("mpt-admin"), eq("REL-1"), any());
    }

    @Test
    @DisplayName("GET /unbind/operations/{id}：返回权威台账状态")
    void operationRoute() throws Exception {
        when(queryAppService.queryOperation("UBO-1")).thenReturn(MptUnbindOperationResult.builder()
                .operationId("UBO-1").relationId("REL-1").vin("LSVAA1234567890AB")
                .relationState(UnbindRelationState.RELEASED)
                .credentialRevocationStatus(CredentialRevocationStatus.FAILED)
                .retryCount(3).build());

        mockMvc.perform(get("/api/mpt/userVehicle/v1/unbind/operations/UBO-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.relationState").value("RELEASED"))
                .andExpect(jsonPath("$.data.credentialRevocationStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.retryCount").value(3));
    }

    @Test
    @DisplayName("POST /unbind/operations/{id}/retry-revocation：人工重试沿用原 operationId")
    void retryRoute() throws Exception {
        when(unbindAppService.retryRevocation("mpt-admin", "UBO-1")).thenReturn(unbindResult());

        mockMvc.perform(post("/api/mpt/userVehicle/v1/unbind/operations/UBO-1/retry-revocation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operationId").value("UBO-1"))
                .andExpect(jsonPath("$.data.relationState").value("RELEASED"));
        verify(unbindAppService).retryRevocation("mpt-admin", "UBO-1");
    }
}
