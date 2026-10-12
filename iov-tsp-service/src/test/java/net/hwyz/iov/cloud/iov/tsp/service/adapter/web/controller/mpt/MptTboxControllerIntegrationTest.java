package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.mpt;

import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxBindingStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxTimelineMilestone;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptTboxDetailResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptTboxSummaryResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptTboxTimelineResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.TboxLinkKeysResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.TboxTimelineItemResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.MptTboxQueryAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MPT TBOX 控制器集成测试（standalone MockMvc，TSP-DSN-CR-009）
 */
class MptTboxControllerIntegrationTest {

    private MptTboxQueryAppService appService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        appService = mock(MptTboxQueryAppService.class);
        MptTboxController controller = new MptTboxController(appService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private MptTboxSummaryResult summary() {
        return MptTboxSummaryResult.builder()
                .sn("SN-1")
                .vin("LSVAA2180C2000001")
                .deviceStatus(2)
                .bindingStatus(TboxBindingStatus.BOUND)
                .firstLoginTime(LocalDateTime.of(2026, 9, 2, 8, 0, 0))
                .lastLoginTime(LocalDateTime.of(2026, 9, 10, 8, 0, 0))
                .maskedHsmUid("HSM1****5678")
                .maskedIccids(List.of("898600**********1234"))
                .build();
    }

    @Test
    void list_ShouldReturnPagedItems() throws Exception {
        when(appService.search(any())).thenReturn(PageResult.<MptTboxSummaryResult>builder()
                .items(List.of(summary())).total(1).page(1).size(20).build());

        mockMvc.perform(get("/api/mpt/tbox/v1/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].sn").value("SN-1"))
                .andExpect(jsonPath("$.data.items[0].maskedHsmUid").value("HSM1****5678"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void detail_ShouldReturnSummaryAndLinkKeys() throws Exception {
        when(appService.getDetail("SN-1")).thenReturn(MptTboxDetailResult.builder()
                .summary(summary())
                .vehicleNodeCode("NODE-1")
                .bindingState("ACTIVE")
                .linkKeys(TboxLinkKeysResult.builder().sn("SN-1").vin("LSVAA2180C2000001").build())
                .build());

        mockMvc.perform(get("/api/mpt/tbox/v1/SN-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.sn").value("SN-1"))
                .andExpect(jsonPath("$.data.linkKeys.vin").value("LSVAA2180C2000001"));
    }

    @Test
    void timeline_ShouldReturnPartialFlagAndMilestones() throws Exception {
        when(appService.getTimeline("SN-1")).thenReturn(MptTboxTimelineResult.builder()
                .partial(true)
                .milestones(List.of(TboxTimelineItemResult.builder()
                        .milestone(TboxTimelineMilestone.FIRST_LOGIN)
                        .occurredAt(LocalDateTime.of(2026, 9, 2, 8, 0, 0))
                        .build()))
                .build());

        mockMvc.perform(get("/api/mpt/tbox/v1/SN-1/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partial").value(true))
                .andExpect(jsonPath("$.data.milestones[0].milestone").value("FIRST_LOGIN"));
    }

    @Test
    void export_ShouldRouteToExportEndpoint() throws Exception {
        when(appService.export(any())).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/mpt/tbox/v1/export"))
                .andExpect(status().isOk());

        verify(appService).export(any());
    }
}
