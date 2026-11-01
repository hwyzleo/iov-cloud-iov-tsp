package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxAnomalyType;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxBindingStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.MptTboxQueryRequest;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptTboxDetailResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptTboxSummaryResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptTboxTimelineResult;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.MptTboxErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxManagementQueryRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxRegistrationEventRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxManagementCriteria;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxManagementView;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query.TboxAnomalyEvaluator;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query.TboxManagementQueryService;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query.TboxTimelineBuilder;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TboxManagementProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MPT TBOX 查询应用服务测试（TSP-DSN-CR-009）
 */
class MptTboxQueryAppServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private TboxManagementQueryService queryService;
    private TboxRegistrationEventRepository registrationEventRepository;
    private TboxManagementProperties properties;
    private MptTboxQueryAppService appService;

    @BeforeEach
    void setUp() {
        queryService = mock(TboxManagementQueryService.class);
        registrationEventRepository = mock(TboxRegistrationEventRepository.class);
        properties = new TboxManagementProperties();
        properties.setActivationTimeout(Duration.ofMinutes(30));
        properties.setLongOfflineThreshold(Duration.ofDays(30));
        properties.setExportMaxRows(2);
        properties.setDefaultPageSize(20);
        properties.setMaxPageSize(200);
        appService = new MptTboxQueryAppService(queryService, new TboxAnomalyEvaluator(),
                new TboxTimelineBuilder(), registrationEventRepository, properties);
    }

    private TboxManagementView view(String sn) {
        return TboxManagementView.builder()
                .sn(sn)
                .hsm("HSM12345678")
                .imei("IMEI123456789012")
                .iccid1("89860012345678901234")
                .deviceStatus(2)
                .bindCount(1)
                .vin("LSVAA2180C2000001")
                .partCode("PART-1")
                .firstLoginTime(NOW.minusDays(1))
                .lastLoginTime(NOW.minusHours(1))
                .activateTime(NOW.minusHours(23))
                .createTime(NOW.minusDays(5))
                .modifyTime(NOW)
                .build();
    }

    @Test
    void search_ShouldMaskSensitiveFieldsAndReturnPage() {
        when(queryService.search(any())).thenReturn(List.of(view("SN-1"), view("SN-2")));

        PageResult<MptTboxSummaryResult> page = appService.search(MptTboxQueryRequest.builder().build(), SHANGHAI);

        assertEquals(2, page.getItems().size());
        assertEquals(2, page.getTotal());
        MptTboxSummaryResult first = page.getItems().get(0);
        assertEquals("HSM1***5678", first.getMaskedHsmUid());
        assertEquals("IMEI********9012", first.getMaskedImei());
        assertEquals(List.of("898600**********1234"), first.getMaskedIccids());
        assertEquals(TboxBindingStatus.BOUND, first.getBindingStatus());
        assertNotNull(first.getActivationDurationSeconds());
        assertNotNull(first.getLastLoginAgeSeconds());
    }

    @Test
    void search_UnboundView_ShouldDeriveUnboundStatus() {
        TboxManagementView unbound = view("SN-3");
        unbound.setBindCount(0);
        unbound.setVin(null);
        when(queryService.search(any())).thenReturn(List.of(unbound));

        MptTboxSummaryResult result = appService.search(MptTboxQueryRequest.builder().build(), SHANGHAI).getItems().get(0);

        assertEquals(TboxBindingStatus.UNBOUND, result.getBindingStatus());
        assertTrue(result.getAnomalyTags().stream().anyMatch(t -> t.getType() == TboxAnomalyType.UNBOUND));
    }

    @Test
    void search_InvalidSortBy_ShouldThrowInvalidQueryParam() {
        MptTboxQueryRequest request = MptTboxQueryRequest.builder().sortBy("evil;drop table").build();

        BusinessException ex = assertThrows(BusinessException.class, () -> appService.search(request, SHANGHAI));
        assertEquals(MptTboxErrorCode.INVALID_QUERY_PARAM, ex.getErrorCode());
    }

    @Test
    void getDetail_ShouldAssembleSummaryBindingAndMilestones() {
        when(queryService.getBySn("SN-1")).thenReturn(view("SN-1"));
        when(registrationEventRepository.listByDeviceSn("SN-1")).thenReturn(List.of());

        MptTboxDetailResult detail = appService.getDetail("SN-1");

        assertEquals("SN-1", detail.getSummary().getSn());
        assertEquals("ACTIVE", detail.getBindingState());
        assertEquals("LSVAA2180C2000001", detail.getLinkKeys().getVin());
        assertFalse(detail.getActivationMilestones().isEmpty());
    }

    @Test
    void getTimeline_InboxFailure_ShouldMarkPartialAndStillReturnFieldMilestones() {
        when(queryService.getBySn("SN-1")).thenReturn(view("SN-1"));
        when(registrationEventRepository.listByDeviceSn("SN-1")).thenThrow(new RuntimeException("inbox down"));

        MptTboxTimelineResult result = appService.getTimeline("SN-1");

        assertTrue(result.isPartial());
        assertFalse(result.getMilestones().isEmpty());
    }

    @Test
    void export_WithinLimit_ShouldReturnItems() {
        when(queryService.search(any())).thenReturn(List.of(view("SN-1")));

        List<MptTboxSummaryResult> items = appService.export(MptTboxQueryRequest.builder().build(), SHANGHAI);

        assertEquals(1, items.size());
    }

    @Test
    void export_OverLimit_ShouldThrowExportLimitExceeded() {
        when(queryService.search(any())).thenReturn(List.of(view("SN-1"), view("SN-2"), view("SN-3")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> appService.export(MptTboxQueryRequest.builder().build(), SHANGHAI));
        assertEquals(MptTboxErrorCode.EXPORT_LIMIT_EXCEEDED, ex.getErrorCode());
    }

    @Test
    void pageSize_OverMax_ShouldBeClamped() {
        when(queryService.search(any())).thenReturn(List.of(view("SN-1")));

        PageResult<MptTboxSummaryResult> page = appService.search(
                MptTboxQueryRequest.builder().pageNo(1).pageSize(9999).build(), SHANGHAI);

        assertEquals(200, page.getSize());
    }

    // ---------- TSP-DSN-CR-015 RD-MPT-TZ-004：时间筛选对称解析 ----------

    @Test
    void search_ShanghaiTimeRange_ShouldConvertToUtcWallClockCriteria() {
        when(queryService.search(any())).thenReturn(List.of(view("SN-1")));

        appService.search(MptTboxQueryRequest.builder()
                .lastLoginFrom("2026-11-01T15:00:00")
                .lastLoginTo("2026-11-01T16:00:00")
                .build(), SHANGHAI);

        ArgumentCaptor<TboxManagementCriteria> captor = ArgumentCaptor.forClass(TboxManagementCriteria.class);
        verify(queryService).search(captor.capture());
        assertEquals(LocalDateTime.of(2026, 11, 1, 7, 0, 0), captor.getValue().getLastLoginFrom());
        assertEquals(LocalDateTime.of(2026, 11, 1, 8, 0, 0), captor.getValue().getLastLoginTo());
    }

    @Test
    void search_UtcZoneTimeRange_ShouldKeepWallClock() {
        when(queryService.search(any())).thenReturn(List.of(view("SN-1")));

        appService.search(MptTboxQueryRequest.builder()
                .lastLoginFrom("2026-11-01T15:00:00")
                .build(), ZoneId.of("UTC"));

        ArgumentCaptor<TboxManagementCriteria> captor = ArgumentCaptor.forClass(TboxManagementCriteria.class);
        verify(queryService).search(captor.capture());
        assertEquals(LocalDateTime.of(2026, 11, 1, 15, 0, 0), captor.getValue().getLastLoginFrom());
    }

    @Test
    void search_InvalidOrReversedTimeRange_ShouldThrowInvalidQueryParam() {
        BusinessException invalid = assertThrows(BusinessException.class,
                () -> appService.search(MptTboxQueryRequest.builder()
                        .lastLoginFrom("not-a-time").build(), SHANGHAI));
        assertEquals(MptTboxErrorCode.INVALID_QUERY_PARAM, invalid.getErrorCode());

        BusinessException reversed = assertThrows(BusinessException.class,
                () -> appService.search(MptTboxQueryRequest.builder()
                        .lastLoginFrom("2026-11-01T16:00:00")
                        .lastLoginTo("2026-11-01T15:00:00").build(), SHANGHAI));
        assertEquals(MptTboxErrorCode.INVALID_QUERY_PARAM, reversed.getErrorCode());
    }

    @Test
    void search_DstGapWithoutOffset_ShouldReject() {
        // Berlin 2026-03-29T02:30 处于 spring-forward gap，无 offset 必须拒绝
        BusinessException ex = assertThrows(BusinessException.class,
                () -> appService.search(MptTboxQueryRequest.builder()
                        .lastLoginFrom("2026-03-29T02:30:00").build(), ZoneId.of("Europe/Berlin")));
        assertEquals(MptTboxErrorCode.INVALID_QUERY_PARAM, ex.getErrorCode());
    }

    @Test
    void search_DstOverlapWithoutOffset_ShouldReject() {
        // Berlin 2026-10-25T02:30 秋季重叠，无 offset 必须拒绝
        BusinessException ex = assertThrows(BusinessException.class,
                () -> appService.search(MptTboxQueryRequest.builder()
                        .lastLoginFrom("2026-10-25T02:30:00").build(), ZoneId.of("Europe/Berlin")));
        assertEquals(MptTboxErrorCode.INVALID_QUERY_PARAM, ex.getErrorCode());
    }

    @Test
    void search_DstOverlapWithExplicitOffset_ShouldResolve() {
        // Berlin 2026-10-25T02:30 +02:00 → UTC 墙钟 2026-10-25T00:30
        when(queryService.search(any())).thenReturn(List.of(view("SN-1")));
        appService.search(MptTboxQueryRequest.builder()
                .lastLoginFrom("2026-10-25T02:30:00+02:00").build(), ZoneId.of("Europe/Berlin"));
        ArgumentCaptor<TboxManagementCriteria> captor = ArgumentCaptor.forClass(TboxManagementCriteria.class);
        verify(queryService).search(captor.capture());
        assertEquals(LocalDateTime.of(2026, 10, 25, 0, 30, 0), captor.getValue().getLastLoginFrom());
    }
}
