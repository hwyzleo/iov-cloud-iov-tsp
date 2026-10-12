package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxAnomalyType;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxBindingStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.MptTboxQueryRequest;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.MptTboxErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.SensitiveDataMasker;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxRegistrationEventRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxManagementCriteria;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxManagementView;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxRegistrationEventRecord;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query.TboxAnomalyEvaluator;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query.TboxManagementQueryService;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query.TboxTimelineBuilder;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query.model.AnomalyTag;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query.model.TimelineMilestone;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TboxManagementProperties;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptTboxDetailResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptTboxSummaryResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptTboxTimelineResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.TboxAnomalyTagResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.TboxLinkKeysResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.TboxTimelineItemResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * MPT TBOX 后台运营查询应用服务
 * <p>
 * TSP-DSN-CR-009：只读查询/详情/时间线/导出；使用只读事务，不获取生命周期写锁，
 * 不调用 VMD/MDM/PKI/KMS。查询审计以结构化日志记录操作者、过滤摘要、目标、条数与耗时。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MptTboxQueryAppService {

    private static final Set<String> SORT_WHITELIST = Set.of(
            "lastLoginTime", "firstLoginTime", "activateTime", "createdAt", "updatedAt", "deviceStatus");
    private static final String DEFAULT_SORT_BY = "lastLoginTime";
    private static final Set<String> SORT_DIRECTIONS = Set.of("asc", "desc");

    private final TboxManagementQueryService tboxManagementQueryService;
    private final TboxAnomalyEvaluator tboxAnomalyEvaluator;
    private final TboxTimelineBuilder tboxTimelineBuilder;
    private final TboxRegistrationEventRepository tboxRegistrationEventRepository;
    private final TboxManagementProperties tboxManagementProperties;

    /**
     * 分页查询 TBOX 列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    public PageResult<MptTboxSummaryResult> search(MptTboxQueryRequest request) {
        long startNanos = System.nanoTime();
        LocalDateTime now = LocalDateTime.now();
        TboxManagementCriteria criteria = buildCriteria(request, now);
        int pageNo = normalizePageNo(request);
        int pageSize = normalizePageSize(request);

        PageUtil.startPage(pageNo, pageSize);
        List<TboxManagementView> views;
        try {
            views = tboxManagementQueryService.search(criteria);
            PageResult<TboxManagementView> raw = PageUtil.getPageResult(views);
            List<MptTboxSummaryResult> items = toSummaries(views, now);
            audit("list", request, null, raw.getTotal(), startNanos);
            return PageResult.<MptTboxSummaryResult>builder()
                    .items(items)
                    .total(raw.getTotal())
                    .page(pageNo)
                    .size(pageSize)
                    .build();
        } finally {
            PageUtil.clearPage();
        }
    }

    /**
     * 查询 TBOX 详情
     *
     * @param sn 设备序列号
     * @return 详情结果
     */
    @Transactional(readOnly = true)
    public MptTboxDetailResult getDetail(String sn) {
        long startNanos = System.nanoTime();
        LocalDateTime now = LocalDateTime.now();
        TboxManagementView view = tboxManagementQueryService.getBySn(sn);
        List<AnomalyTag> tags = evaluateAnomalies(view, now);
        List<TboxRegistrationEventRecord> events = loadInboxQuietly(view.getSn());
        List<TimelineMilestone> milestones = tboxTimelineBuilder.build(view, events);

        MptTboxSummaryResult summary = toSummary(view, now, tags);
        MptTboxDetailResult detail = MptTboxDetailResult.builder()
                .summary(summary)
                .hardwareNo(view.getHardwareNo())
                .softwareNo(view.getSoftwareNo())
                .configWord(view.getConfigWord())
                .vehicleNodeCode(view.getVehicleNodeCode())
                .bindingId(view.getBindingId())
                .bindingState(resolveBindingState(view))
                .bindingUpdatedAt(view.getBindingUpdatedAt())
                .activationMilestones(milestones.stream().map(this::toTimelineResult).toList())
                .linkKeys(TboxLinkKeysResult.builder()
                        .sn(view.getSn())
                        .partCode(view.getPartCode())
                        .vin(view.getVin())
                        .bindingId(view.getBindingId())
                        .build())
                .build();
        audit("detail", null, view.getSn(), 1, startNanos);
        return detail;
    }

    /**
     * 查询 TBOX 生命周期时间线
     *
     * @param sn 设备序列号
     * @return 时间线结果，Inbox 不可用时 partial=true
     */
    @Transactional(readOnly = true)
    public MptTboxTimelineResult getTimeline(String sn) {
        long startNanos = System.nanoTime();
        TboxManagementView view = tboxManagementQueryService.getBySn(sn);
        boolean partial = false;
        List<TboxRegistrationEventRecord> events;
        try {
            events = tboxRegistrationEventRepository.listByDeviceSn(view.getSn());
        } catch (Exception e) {
            partial = true;
            events = null;
            log.warn("读取 TBOX 消费幂等记录失败，返回部分时间线, sn={}", maskSummary(view.getSn()), e);
        }
        List<TimelineMilestone> milestones = tboxTimelineBuilder.build(view, events);
        audit("timeline", null, view.getSn(), milestones.size(), startNanos);
        return MptTboxTimelineResult.builder()
                .partial(partial)
                .milestones(milestones.stream().map(this::toTimelineResult).toList())
                .build();
    }

    /**
     * 按列表条件导出脱敏结果（超出行数限制时抛出稳定错误）
     *
     * @param request 查询请求
     * @return 导出项列表
     */
    @Transactional(readOnly = true)
    public List<MptTboxSummaryResult> export(MptTboxQueryRequest request) {
        long startNanos = System.nanoTime();
        LocalDateTime now = LocalDateTime.now();
        int maxRows = Math.max(tboxManagementProperties.getExportMaxRows(), 1);
        TboxManagementCriteria criteria = buildCriteria(request, now);

        PageUtil.startPage(1, maxRows + 1);
        try {
            List<TboxManagementView> views = tboxManagementQueryService.search(criteria);
            PageResult<TboxManagementView> raw = PageUtil.getPageResult(views);
            if (raw.getTotal() > maxRows) {
                log.warn("TBOX 导出超过平台限制, total={}, maxRows={}", raw.getTotal(), maxRows);
                throw new BusinessException(MptTboxErrorCode.EXPORT_LIMIT_EXCEEDED);
            }
            List<MptTboxSummaryResult> items = toSummaries(views, now);
            audit("export", request, null, raw.getTotal(), startNanos);
            return items;
        } finally {
            PageUtil.clearPage();
        }
    }

    private TboxManagementCriteria buildCriteria(MptTboxQueryRequest request, LocalDateTime now) {
        return TboxManagementCriteria.builder()
                .sn(trimToNull(request.getSn()))
                .vin(trimToNull(request.getVin()))
                .partCode(trimToNull(request.getPartCode()))
                .deviceStatus(request.getDeviceStatus())
                .bindingStatus(parseBindingStatusName(request.getBindingStatus()))
                .anomalyType(parseAnomalyTypeName(request.getAnomalyType()))
                .firstLoginFrom(request.getFirstLoginFrom())
                .firstLoginTo(request.getFirstLoginTo())
                .lastLoginFrom(request.getLastLoginFrom())
                .lastLoginTo(request.getLastLoginTo())
                .activateFrom(request.getActivateFrom())
                .activateTo(request.getActivateTo())
                .createdFrom(request.getCreatedFrom())
                .createdTo(request.getCreatedTo())
                .activationTimeoutBefore(now.minus(tboxManagementProperties.getActivationTimeout()))
                .longOfflineBefore(now.minus(tboxManagementProperties.getLongOfflineThreshold()))
                .sortBy(normalizeSortBy(request.getSortBy()))
                .sortAsc(normalizeSortAsc(request.getSortDirection()))
                .build();
    }

    private List<MptTboxSummaryResult> toSummaries(List<TboxManagementView> views, LocalDateTime now) {
        List<MptTboxSummaryResult> items = new ArrayList<>(views.size());
        for (TboxManagementView view : views) {
            items.add(toSummary(view, now, evaluateAnomalies(view, now)));
        }
        return items;
    }

    private MptTboxSummaryResult toSummary(TboxManagementView view, LocalDateTime now, List<AnomalyTag> tags) {
        List<TboxAnomalyTagResult> tagResults = tags.stream().map(this::toAnomalyResult).toList();
        return MptTboxSummaryResult.builder()
                .sn(view.getSn())
                .vin(view.getVin())
                .partCode(view.getPartCode())
                .deviceStatus(view.getDeviceStatus())
                .firstLoginTime(view.getFirstLoginTime())
                .lastLoginTime(view.getLastLoginTime())
                .activateTime(view.getActivateTime())
                .bindingStatus(resolveBindingStatus(view))
                .bindingVersion(view.getBindingVersion())
                .anomalyTags(tagResults)
                .primaryAnomalyType(tagResults.isEmpty() ? null : tagResults.get(0).getType())
                .activationDurationSeconds(activationDurationSeconds(view))
                .lastLoginAgeSeconds(lastLoginAgeSeconds(view, now))
                .maskedHsmUid(SensitiveDataMasker.maskHsmUid(view.getHsm()))
                .maskedImei(SensitiveDataMasker.maskImei(view.getImei()))
                .maskedIccids(maskedIccids(view))
                .supplierCode(view.getSupplierCode())
                .hardwareVersion(view.getHardwareVer())
                .softwareVersion(view.getSoftwareVer())
                .createdAt(view.getCreateTime())
                .updatedAt(view.getModifyTime())
                .build();
    }

    private List<AnomalyTag> evaluateAnomalies(TboxManagementView view, LocalDateTime now) {
        return tboxAnomalyEvaluator.evaluate(view, now,
                tboxManagementProperties.getActivationTimeout(),
                tboxManagementProperties.getLongOfflineThreshold());
    }

    private List<TboxRegistrationEventRecord> loadInboxQuietly(String sn) {
        try {
            return tboxRegistrationEventRepository.listByDeviceSn(sn);
        } catch (Exception e) {
            log.warn("读取 TBOX 消费幂等记录失败，详情省略事件来源里程碑, sn={}", maskSummary(sn), e);
            return null;
        }
    }

    private TboxAnomalyTagResult toAnomalyResult(AnomalyTag tag) {
        return TboxAnomalyTagResult.builder()
                .type(tag.type())
                .evaluatedAt(tag.evaluatedAt())
                .since(tag.since())
                .durationSeconds(tag.durationSeconds())
                .build();
    }

    private TboxTimelineItemResult toTimelineResult(TimelineMilestone milestone) {
        return TboxTimelineItemResult.builder()
                .milestone(milestone.milestone())
                .occurredAt(milestone.occurredAt())
                .sourceRef(milestone.sourceRef())
                .detail(milestone.detail())
                .build();
    }

    private TboxBindingStatus resolveBindingStatus(TboxManagementView view) {
        int bindCount = view.getBindCount() == null ? 0 : view.getBindCount();
        if (bindCount <= 0) {
            return TboxBindingStatus.UNBOUND;
        }
        return bindCount == 1 ? TboxBindingStatus.BOUND : TboxBindingStatus.BINDING_CONFLICT;
    }

    private String resolveBindingState(TboxManagementView view) {
        return TboxBindingStatus.BOUND == resolveBindingStatus(view) ? "ACTIVE" : null;
    }

    private Long activationDurationSeconds(TboxManagementView view) {
        if (view.getFirstLoginTime() == null || view.getActivateTime() == null) {
            return null;
        }
        return ChronoUnit.SECONDS.between(view.getFirstLoginTime(), view.getActivateTime());
    }

    private Long lastLoginAgeSeconds(TboxManagementView view, LocalDateTime now) {
        if (view.getLastLoginTime() == null) {
            return null;
        }
        return ChronoUnit.SECONDS.between(view.getLastLoginTime(), now);
    }

    private List<String> maskedIccids(TboxManagementView view) {
        List<String> iccids = new ArrayList<>(2);
        if (view.getIccid1() != null && !view.getIccid1().isBlank()) {
            iccids.add(SensitiveDataMasker.maskIccid(view.getIccid1()));
        }
        if (view.getIccid2() != null && !view.getIccid2().isBlank()) {
            iccids.add(SensitiveDataMasker.maskIccid(view.getIccid2()));
        }
        return iccids;
    }

    private String parseBindingStatusName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return TboxBindingStatus.valueOf(value.trim().toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(MptTboxErrorCode.INVALID_QUERY_PARAM,
                    "非法的绑定状态: " + value);
        }
    }

    private String parseAnomalyTypeName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return TboxAnomalyType.valueOf(value.trim().toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(MptTboxErrorCode.INVALID_QUERY_PARAM,
                    "非法的异常类型: " + value);
        }
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return DEFAULT_SORT_BY;
        }
        String trimmed = sortBy.trim();
        if (!SORT_WHITELIST.contains(trimmed)) {
            throw new BusinessException(MptTboxErrorCode.INVALID_QUERY_PARAM,
                    "非法的排序字段: " + sortBy);
        }
        return trimmed;
    }

    private boolean normalizeSortAsc(String sortDirection) {
        if (sortDirection == null || sortDirection.isBlank()) {
            return false;
        }
        String trimmed = sortDirection.trim().toLowerCase();
        if (!SORT_DIRECTIONS.contains(trimmed)) {
            throw new BusinessException(MptTboxErrorCode.INVALID_QUERY_PARAM,
                    "非法的排序方向: " + sortDirection);
        }
        return "asc".equals(trimmed);
    }

    private int normalizePageNo(MptTboxQueryRequest request) {
        Integer pageNo = request.getPageNo();
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    private int normalizePageSize(MptTboxQueryRequest request) {
        int defaultSize = tboxManagementProperties.getDefaultPageSize();
        int maxSize = tboxManagementProperties.getMaxPageSize();
        Integer pageSize = request.getPageSize();
        if (pageSize == null || pageSize < 1) {
            return defaultSize;
        }
        if (pageSize > maxSize) {
            log.warn("TBOX 查询 pageSize[{}] 超过最大值[{}]，已截断", pageSize, maxSize);
            return maxSize;
        }
        return pageSize;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void audit(String action, MptTboxQueryRequest request, String targetSn, long count, long startNanos) {
        long costMs = (System.nanoTime() - startNanos) / 1_000_000;
        log.info("TBOX运营查询审计 action={}, operator={}, sn={}, vin={}, partCode={}, deviceStatus={}, "
                        + "bindingStatus={}, anomalyType={}, targetSn={}, resultCount={}, costMs={}",
                action, currentOperator(),
                request != null ? maskSummary(request.getSn()) : null,
                request != null ? maskSummary(request.getVin()) : null,
                request != null ? request.getPartCode() : null,
                request != null ? request.getDeviceStatus() : null,
                request != null ? request.getBindingStatus() : null,
                request != null ? request.getAnomalyType() : null,
                maskSummary(targetSn), count, costMs);
    }

    private String currentOperator() {
        try {
            return SecurityContextHolder.getUserName();
        } catch (Exception e) {
            return null;
        }
    }

    private String maskSummary(String value) {
        return SensitiveDataMasker.mask(value, 4, 4);
    }
}
