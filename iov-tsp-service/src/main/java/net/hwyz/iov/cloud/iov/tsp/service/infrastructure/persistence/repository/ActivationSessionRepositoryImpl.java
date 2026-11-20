package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ActivationSessionState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.IviLoginResultStatus;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.UtcTime;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationSession;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.ActivationSessionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.ActivationSessionMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.ActivationSessionPo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * 一次性激活会话仓储实现（时间列按 UTC 绝对时刻显式映射）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ActivationSessionRepositoryImpl implements ActivationSessionRepository {

    private final ActivationSessionMapper activationSessionMapper;

    @Override
    public void insert(ActivationSession session) {
        ActivationSessionPo po = ActivationSessionPo.builder()
                .sessionId(session.getSessionId())
                .qrTokenHash(session.getQrTokenHash())
                .trustedVehicleRef(session.getTrustedVehicleRef())
                .vin(session.getVin())
                .agreementRevisions(session.getAgreementRevisions())
                .expiresAt(UtcTime.toLocalDateTime(session.getExpiresAt()))
                .state(session.getState() == null ? ActivationSessionState.ISSUED.name() : session.getState().name())
                .claimedAccountId(session.getClaimedAccountId())
                .claimedAt(UtcTime.toLocalDateTime(session.getClaimedAt()))
                .bindingOperationId(session.getBindingOperationId())
                .resultExpiresAt(UtcTime.toLocalDateTime(session.getResultExpiresAt()))
                .iviLoginState(session.getIviLoginState() == null ? null : session.getIviLoginState().name())
                .build();
        activationSessionMapper.insert(po);
        session.setId(po.getId());
    }

    @Override
    public Optional<ActivationSession> findBySessionId(String sessionId) {
        return Optional.ofNullable(activationSessionMapper.selectBySessionId(sessionId)).map(this::toEntity);
    }

    @Override
    public Optional<ActivationSession> findByQrTokenHash(String qrTokenHash) {
        return Optional.ofNullable(activationSessionMapper.selectByQrTokenHash(qrTokenHash)).map(this::toEntity);
    }

    @Override
    public Optional<ActivationSession> lockBySessionId(String sessionId) {
        return Optional.ofNullable(activationSessionMapper.selectBySessionIdForUpdate(sessionId)).map(this::toEntity);
    }

    @Override
    public boolean claim(String sessionId, String accountId, Instant now, int expectedVersion) {
        return activationSessionMapper.claim(sessionId, accountId, UtcTime.toLocalDateTime(now), expectedVersion) > 0;
    }

    @Override
    public boolean cancel(String sessionId, String accountId, Instant now, int expectedVersion) {
        return activationSessionMapper.cancel(sessionId, accountId, UtcTime.toLocalDateTime(now), expectedVersion) > 0;
    }

    @Override
    public boolean markBound(String sessionId, String operationId, Instant now, Instant resultExpiresAt,
                             int expectedVersion) {
        return activationSessionMapper.markBound(sessionId, operationId, UtcTime.toLocalDateTime(now),
                UtcTime.toLocalDateTime(resultExpiresAt), expectedVersion) > 0;
    }

    @Override
    public boolean markSkipped(String sessionId, Instant now, int expectedVersion) {
        return activationSessionMapper.markSkipped(sessionId, UtcTime.toLocalDateTime(now), expectedVersion) > 0;
    }

    @Override
    public boolean reportIviLoginResult(String sessionId, IviLoginResultStatus status, String reportId, Instant now) {
        return activationSessionMapper.reportIviLoginResult(sessionId, status.name(), reportId,
                UtcTime.toLocalDateTime(now)) > 0;
    }

    private ActivationSession toEntity(ActivationSessionPo po) {
        return ActivationSession.builder()
                .id(po.getId())
                .sessionId(po.getSessionId())
                .qrTokenHash(po.getQrTokenHash())
                .trustedVehicleRef(po.getTrustedVehicleRef())
                .vin(po.getVin())
                .agreementRevisions(po.getAgreementRevisions())
                .expiresAt(UtcTime.toInstant(po.getExpiresAt()))
                .state(po.getState() == null ? null : ActivationSessionState.valueOf(po.getState()))
                .claimedAccountId(po.getClaimedAccountId())
                .claimedAt(UtcTime.toInstant(po.getClaimedAt()))
                .bindingOperationId(po.getBindingOperationId())
                .skippedAt(UtcTime.toInstant(po.getSkippedAt()))
                .cancelledAt(UtcTime.toInstant(po.getCancelledAt()))
                .boundAt(UtcTime.toInstant(po.getBoundAt()))
                .resultExpiresAt(UtcTime.toInstant(po.getResultExpiresAt()))
                .iviLoginState(po.getIviLoginState() == null ? null : IviLoginResultStatus.valueOf(po.getIviLoginState()))
                .iviLoginReportId(po.getIviLoginReportId())
                .iviLoginReportedAt(UtcTime.toInstant(po.getIviLoginReportedAt()))
                .rowVersion(po.getRowVersion())
                .build();
    }
}
