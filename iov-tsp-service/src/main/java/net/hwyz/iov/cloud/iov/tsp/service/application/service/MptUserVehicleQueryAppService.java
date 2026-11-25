package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.MptUserVehicleDetailStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UserVehicleBindingStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.MptUserVehicleHistoryRequest;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUnbindOperationResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUserVehicleHistoryResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUserVehicleRelationResult;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.UserVehicleBindingErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.VinNormalizer;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * MPT 车辆详情用户关系查询应用服务（IOV-TSP-DSN-CR-021 D021-02/D021-09）。
 * <p>
 * 三个事实区独立读取、独立表达：
 * <ul>
 *   <li>用户绑定：TSP 本地关系权威（ACTIVE 优先，无 ACTIVE 时返回最近 RELEASED 作为历史，bindingStatus=UNBOUND）；</li>
 *   <li>车机登录：经 {@code sourceOperationId → bindingOperation.sessionId → activationSession} 读取，不回退推演；</li>
 *   <li>首次激活：VMD 官方只读契约；失败映射 UNKNOWN + detailStatus=PARTIAL，不以 Outbox 状态冒充。</li>
 * </ul>
 * 历史列表由专用 Query Repository 在 SQL 侧过滤与物理分页；账号等标识由 Adapter 脱敏。
 * 本服务只读，不写关系/审计/授权，不调用除 VMD 只读外的外部服务。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MptUserVehicleQueryAppService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    private final UserVehicleRelationRepository userVehicleRelationRepository;
    private final UserVehicleUnbindOperationRepository userVehicleUnbindOperationRepository;
    private final BindingOperationRepository bindingOperationRepository;
    private final ActivationSessionRepository activationSessionRepository;
    private final MptUserVehicleQueryRepository mptUserVehicleQueryRepository;
    private final FirstActivationQueryPort firstActivationQueryPort;

    /**
     * 查询车辆详情「用户绑定/激活」数据区。
     * <p>
     * 不开启数据库事务：VMD 只读 RPC 不得在事务/连接持有期间执行；各本地读均为单语句。
     *
     * @param vin 规范化 VIN
     * @return 查询结果（时间保持 UTC 事实）
     */
    public MptUserVehicleRelationResult getRelation(String vin) {
        String normalizedVin = VinNormalizer.normalize(vin);
        if (!VinNormalizer.isValid(normalizedVin)) {
            throw business(UserVehicleBindingErrorCode.INVALID_REQUEST);
        }
        Optional<UserVehicleRelation> active = userVehicleRelationRepository.findActiveByVin(normalizedVin);
        Optional<UserVehicleRelation> latestReleased = active.isEmpty()
                ? userVehicleRelationRepository.findLatestReleasedByVin(normalizedVin)
                : Optional.empty();

        FirstActivationFact activation;
        MptUserVehicleDetailStatus detailStatus = MptUserVehicleDetailStatus.COMPLETE;
        try {
            activation = firstActivationQueryPort.load(normalizedVin);
        } catch (DependencyUnavailableException e) {
            // 外部只读依赖不可用：核心关系详情仍可用，首次激活显式 UNKNOWN
            activation = FirstActivationFact.unknown();
            detailStatus = MptUserVehicleDetailStatus.PARTIAL;
        }

        UserVehicleRelation displayed = active.orElseGet(() -> latestReleased.orElse(null));
        MptUserVehicleRelationResult.MptUserVehicleRelationResultBuilder builder = MptUserVehicleRelationResult.builder()
                .vin(normalizedVin)
                .bindingStatus(active.isPresent() ? UserVehicleBindingStatus.BOUND : UserVehicleBindingStatus.UNBOUND)
                .detailStatus(detailStatus)
                .firstActivation(MptUserVehicleRelationResult.FirstActivationResult.builder()
                        .state(activation.state())
                        .firstActivatedAt(activation.firstActivatedAt())
                        .build());
        if (active.isPresent()) {
            builder.relation(toRelationResult(active.get()));
        } else if (latestReleased.isPresent()) {
            builder.latestReleasedRelation(toLatestReleasedResult(latestReleased.get()));
        }
        if (displayed != null) {
            builder.iviLogin(resolveIviLogin(displayed))
                    .latestUnbind(resolveLatestUnbind(displayed));
        }
        return builder.build();
    }

    /**
     * 查询解绑操作权威结果（查询不到不得被解释为失败，但仍按参数错误返回）
     *
     * @param operationId 解绑操作ID
     * @return 操作结果（UTC 事实）
     */
    @Transactional(readOnly = true)
    public MptUnbindOperationResult queryOperation(String operationId) {
        if (operationId == null || operationId.isBlank()) {
            throw business(UserVehicleBindingErrorCode.INVALID_REQUEST);
        }
        UserVehicleUnbindOperation operation = userVehicleUnbindOperationRepository
                .findByOperationId(operationId.trim())
                .orElseThrow(() -> business(UserVehicleBindingErrorCode.INVALID_REQUEST));
        return MptUnbindOperationResult.builder()
                .operationId(operation.getOperationId())
                .relationId(operation.getRelationId())
                .vin(operation.getVin())
                .relationState(operation.getRelationState())
                .credentialRevocationStatus(operation.getCredentialRevocationStatus())
                .revokedSessionCount(operation.getRevokedSessionCount())
                .retryCount(operation.getRetryCount())
                .releasedAt(operation.getReleasedAt())
                .completedAt(operation.getCompletedAt())
                .nextRetryAt(operation.getNextRetryAt())
                .lastError(operation.getLastError())
                .build();
    }

    /**
     * 分页查询关系历史（数据库侧过滤与物理分页）
     *
     * @param request 查询请求
     * @return 分页结果（UTC 事实）
     */
    @Transactional(readOnly = true)
    public PageResult<MptUserVehicleHistoryResult> searchHistory(MptUserVehicleHistoryRequest request) {
        String vin = null;
        if (request != null && request.getVin() != null && !request.getVin().isBlank()) {
            if (!VinNormalizer.isValid(request.getVin())) {
                throw business(UserVehicleBindingErrorCode.INVALID_REQUEST);
            }
            vin = VinNormalizer.normalize(request.getVin());
        }
        String relationId = trimToNull(request == null ? null : request.getRelationId());
        String operationId = trimToNull(request == null ? null : request.getOperationId());
        int pageNo = normalizePageNo(request == null ? null : request.getPageNo());
        int pageSize = normalizePageSize(request == null ? null : request.getPageSize());

        MptUserVehicleHistoryCriteria criteria = new MptUserVehicleHistoryCriteria(vin, relationId, operationId);
        PageUtil.startPage(pageNo, pageSize);
        try {
            List<MptUserVehicleHistoryItem> items = mptUserVehicleQueryRepository.searchHistory(criteria);
            PageResult<MptUserVehicleHistoryItem> raw = PageUtil.getPageResult(items);
            List<MptUserVehicleHistoryResult> results = items.stream()
                    .map(this::toHistoryResult)
                    .toList();
            return PageResult.<MptUserVehicleHistoryResult>builder()
                    .items(results)
                    .total(raw.getTotal())
                    .page(pageNo)
                    .size(pageSize)
                    .build();
        } finally {
            PageUtil.clearPage();
        }
    }

    // ================= 内部实现 =================

    private MptUserVehicleRelationResult.RelationResult toRelationResult(UserVehicleRelation relation) {
        return MptUserVehicleRelationResult.RelationResult.builder()
                .relationId(relation.getRelationId())
                .accountId(relation.getAccountId())
                .role(relation.getRole())
                .state(relation.getState())
                .boundAt(relation.getBoundAt())
                .releasedAt(relation.getReleasedAt())
                .releaseReason(relation.getReleaseReason())
                .build();
    }

    private MptUserVehicleRelationResult.LatestReleasedRelationResult toLatestReleasedResult(
            UserVehicleRelation relation) {
        return MptUserVehicleRelationResult.LatestReleasedRelationResult.builder()
                .relationId(relation.getRelationId())
                .accountId(relation.getAccountId())
                .boundAt(relation.getBoundAt())
                .releasedAt(relation.getReleasedAt())
                .releaseReason(relation.getReleaseReason())
                .build();
    }

    /**
     * 车机登录事实：经来源绑定操作定位会话；查询失败向上传递，不伪造 SUCCEEDED/NOT_BOUND
     */
    private MptUserVehicleRelationResult.IviLoginResult resolveIviLogin(UserVehicleRelation relation) {
        if (relation.getSourceOperationId() == null || relation.getSourceOperationId().isBlank()) {
            return null;
        }
        Optional<BindingOperation> operation = bindingOperationRepository
                .findByOperationId(relation.getSourceOperationId());
        if (operation.isEmpty() || operation.get().getSessionId() == null) {
            return null;
        }
        Optional<ActivationSession> session = activationSessionRepository
                .findBySessionId(operation.get().getSessionId());
        if (session.isEmpty()) {
            return null;
        }
        ActivationSession value = session.get();
        return MptUserVehicleRelationResult.IviLoginResult.builder()
                .state(value.getIviLoginState())
                .reportedAt(value.getIviLoginReportedAt())
                .build();
    }

    private MptUserVehicleRelationResult.UnbindInfoResult resolveLatestUnbind(UserVehicleRelation relation) {
        if (relation.getReleaseOperationId() == null || relation.getReleaseOperationId().isBlank()) {
            return null;
        }
        return userVehicleUnbindOperationRepository.findByOperationId(relation.getReleaseOperationId())
                .map(operation -> MptUserVehicleRelationResult.UnbindInfoResult.builder()
                        .operationId(operation.getOperationId())
                        .releasedAt(operation.getReleasedAt())
                        .releaseReason(operation.getReason())
                        .credentialRevocationStatus(operation.getCredentialRevocationStatus())
                        .revokedSessionCount(operation.getRevokedSessionCount())
                        .build())
                .orElse(null);
    }

    private MptUserVehicleHistoryResult toHistoryResult(MptUserVehicleHistoryItem item) {
        return MptUserVehicleHistoryResult.builder()
                .relationId(item.relationId())
                .vin(item.vin())
                .accountId(item.accountId())
                .role(item.role())
                .state(item.state())
                .boundAt(item.boundAt())
                .releasedAt(item.releasedAt())
                .releaseReason(item.releaseReason())
                .releaseOperationId(item.releaseOperationId())
                .releasedBy(item.releasedBy())
                .credentialRevocationStatus(item.credentialRevocationStatus())
                .build();
    }

    private int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BusinessException business(UserVehicleBindingErrorCode errorCode) {
        return new BusinessException(errorCode);
    }
}
