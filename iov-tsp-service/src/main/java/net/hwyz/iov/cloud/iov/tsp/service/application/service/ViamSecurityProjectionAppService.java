package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.external.ViamOnboardingClient;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.UtcTime;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Tbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspViamProjectionProperties;
import net.hwyz.iov.cloud.sec.viam.api.event.AccessInvalidatedEvent;
import net.hwyz.iov.cloud.sec.viam.api.event.BusinessKeyChangedEvent;
import net.hwyz.iov.cloud.sec.viam.api.event.DeviceLoginSucceededEvent;
import net.hwyz.iov.cloud.sec.viam.api.event.DeviceOnboardingChangedEvent;
import net.hwyz.iov.cloud.sec.viam.api.vo.DeviceOnboardingVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * VIAM 安全投影应用服务（IOV-TSP-DSN-CR-016 / D016-04）
 * <p>
 * TSP 不再产生安全事实；本服务是 tb_tbox 安全投影列的唯一业务写入者：
 * <ul>
 *   <li>成功接入事件（login-succeeded）：独立更新最近登录事实（lastLoginTime 单调 max）；</li>
 *   <li>状态变化事件（onboarding.changed / access-invalidated / v2c-key-provisioned）：
 *       仅作为「刷新 VIAM 权威快照」的触发，<b>不在 TSP 推断/重建安全状态机</b>（G1~G4 决策）；</li>
 *   <li>快照应用：以 VIAM {@code GET /onboarding/{principalId}} 官方快照为准写
 *       device_status/first_login_time/last_login_time；activate_time 语义未明确（V-GAP-3）
 *       保持未知；securityVersion 当前快照未提供（V-GAP-1）置 NULL=未知。</li>
 * </ul>
 * 资产未导入（tb_tbox 无对应资产）抛出 {@link TboxNotFoundProjectionException}，由消费者
 * 标记 RETRYABLE 待对账，不静默丢弃。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViamSecurityProjectionAppService {

    public static final String SYNC_STATUS_SYNCED = "SYNCED";
    public static final String SYNC_STATUS_PARTIAL = "PARTIAL";
    public static final String SYNC_STATUS_STALE = "STALE";

    /** VIAM 官方安全开通状态 → TSP device_status 码值（与历史枚举码值一致，兼容 MPT 展示） */
    private static final Map<String, Integer> VIAM_STATE_TO_CODE = Map.of(
            "PRE_ACTIVE", 1,
            "ACTIVATING", 5,
            "ACTIVE", 2,
            "BLOCKED", 4,
            "RETIRED", 3);

    private final TboxRepository tboxRepository;
    private final ViamOnboardingClient viamOnboardingClient;
    private final TspViamProjectionProperties properties;

    /**
     * 成功接入事实事件：独立更新最近登录事实（不触发状态推断）。
     * 资产关联键=归一化 hsmUid（tb_tbox.hsm 资产键，非按 VIN 猜测）。
     *
     * @param event 官方事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleLoginSucceeded(DeviceLoginSucceededEvent event) {
        Tbox tbox = requireTboxByHsm(event.hsmUid());
        LocalDateTime occurredAt = UtcTime.toLocalDateTime(event.occurredAt());
        LocalDateTime projectedAt = UtcTime.toLocalDateTime(Instant.now());
        tboxRepository.applyViamLoginFact(tbox.getId(), occurredAt, projectedAt, SYNC_STATUS_PARTIAL);
        log.info("VIAM 投影：最近登录事实已更新 sn={}, occurredAt={}, sync=PARTIAL",
                mask(tbox.getSn()), occurredAt);
    }

    /**
     * 安全开通状态变更事件：仅触发 VIAM 权威快照刷新，不在 TSP 推断状态。
     *
     * @param event 官方事件
     */
    public void handleOnboardingChanged(DeviceOnboardingChangedEvent event) {
        Tbox tbox = requireTboxBySn(event.deviceSn());
        refreshSnapshot(tbox, event.principalId(), event.hsmUid());
    }

    /**
     * 访问失效事件：访问失效执行契约，非完整主体状态契约（G4）——
     * 仅触发 VIAM 权威快照刷新，不按 reason 推断 BLOCKED/RETIRED。
     *
     * @param event 官方事件
     */
    public void handleAccessInvalidated(AccessInvalidatedEvent event) {
        Tbox tbox = resolveTbox(event.getDeviceSn(), event.getHsmUid());
        if (tbox == null) {
            throw new TboxNotFoundProjectionException(event.getDeviceSn(), event.getHsmUid());
        }
        refreshSnapshot(tbox, event.getPrincipalId(), event.getHsmUid());
    }

    /**
     * V2C 密钥开通完成事件：独立密钥开通里程碑（V-GAP-2：不推导 ACTIVE，
     * 不把 occurredAt 填进 activate_time 冒充首次开通时间，V-GAP-3）——
     * 仅触发 VIAM 权威快照刷新。
     *
     * @param event 官方事件
     */
    public void handleV2cKeyProvisioned(BusinessKeyChangedEvent event) {
        Tbox tbox = requireTboxBySn(event.getDeviceSn());
        log.info("VIAM 投影：V2C 密钥开通里程碑（不写 activate_time） sn={}, requestId={}",
                mask(tbox.getSn()), event.getRequestId());
        // V-GAP-2：VIAM onboarding 快照查询按 principalId，需先经 resolveByVin/关联获得；
        // 本 CR 内 V2C 事件仅记录里程碑，状态由后续 onboarding.changed/access-invalidated 触发快照。
    }

    /**
     * 刷新并应用 VIAM 权威快照（fail-closed：快照查询失败保留最后已知，标记 STALE 待对账）。
     */
    void refreshSnapshot(Tbox tbox, String principalId, String hsmUid) {
        if (hsmUid == null && principalId == null) {
            log.warn("VIAM 投影：快照刷新缺少关联键 sn={}, 标记 STALE", mask(tbox.getSn()));
            markStale(tbox.getId());
            return;
        }
        DeviceOnboardingVo snapshot;
        try {
            if (principalId != null) {
                snapshot = viamOnboardingClient.getOnboarding(principalId);
            } else {
                snapshot = viamOnboardingClient.getOnboarding(resolvePrincipalId(hsmUid));
            }
        } catch (Exception e) {
            log.warn("VIAM 投影：快照查询失败，保留最后已知并标记 STALE sn={}, principalId={}, err={}",
                    mask(tbox.getSn()), mask(principalId), e.getClass().getSimpleName());
            markStale(tbox.getId());
            return;
        }
        if (snapshot == null || snapshot.getState() == null) {
            log.warn("VIAM 投影：快照为空/无状态，标记 STALE sn={}", mask(tbox.getSn()));
            markStale(tbox.getId());
            return;
        }
        Integer stateCode = VIAM_STATE_TO_CODE.get(snapshot.getState());
        if (stateCode == null) {
            log.warn("VIAM 投影：未知安全状态 state={}，保留本地 device_status，标记 PARTIAL sn={}",
                    snapshot.getState(), mask(tbox.getSn()));
            tboxRepository.applyViamSnapshot(tbox.getId(), null,
                    UtcTime.toLocalDateTime(snapshot.getFirstLoginTime()),
                    UtcTime.toLocalDateTime(snapshot.getLastLoginTime()),
                    null, UtcTime.toLocalDateTime(Instant.now()), SYNC_STATUS_PARTIAL);
            return;
        }
        // V-GAP-1：当前快照无 securityVersion → 版本列置 NULL=未知；迟到判定依赖 VIAM 后续契约
        tboxRepository.applyViamSnapshot(tbox.getId(), stateCode,
                UtcTime.toLocalDateTime(snapshot.getFirstLoginTime()),
                UtcTime.toLocalDateTime(snapshot.getLastLoginTime()),
                null, UtcTime.toLocalDateTime(Instant.now()), SYNC_STATUS_SYNCED);
        log.info("VIAM 投影：快照已应用 sn={}, state={}, sync=SYNCED",
                mask(tbox.getSn()), snapshot.getState());
    }

    private void markStale(Long id) {
        tboxRepository.applyViamSnapshot(id, null, null, null, null,
                UtcTime.toLocalDateTime(Instant.now()), SYNC_STATUS_STALE);
    }

    private String resolvePrincipalId(String hsmUid) {
        // V-GAP-2：VIAM 无按 hsmUid 的 onboarding 查询端点；此处回退按 hsmUid 反查主体，
        // 当前 VIAM 仅提供 resolveByVin（VIN 维度）。本 CR 内保持 fail-closed：缺 principalId 时标记 STALE。
        throw new TboxNotFoundProjectionException(null, hsmUid);
    }

    private Tbox requireTboxByHsm(String hsmUid) {
        if (hsmUid == null) {
            throw new TboxNotFoundProjectionException(null, null);
        }
        Tbox tbox = tboxRepository.getByHsm(hsmUid);
        if (tbox == null) {
            throw new TboxNotFoundProjectionException(null, hsmUid);
        }
        return tbox;
    }

    private Tbox requireTboxBySn(String deviceSn) {
        Tbox tbox = resolveTbox(deviceSn, null);
        if (tbox == null) {
            throw new TboxNotFoundProjectionException(deviceSn, null);
        }
        return tbox;
    }

    private Tbox resolveTbox(String deviceSn, String hsmUid) {
        if (deviceSn != null) {
            Tbox bySn = tboxRepository.getBySn(deviceSn);
            if (bySn != null) {
                return bySn;
            }
        }
        if (hsmUid != null) {
            return tboxRepository.getByHsm(hsmUid);
        }
        return null;
    }

    private String mask(String value) {
        if (value == null || value.length() <= 8) {
            return value;
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    /**
     * 资产未导入/关联缺失：消费者标记 RETRYABLE 待对账，不静默丢弃。
     */
    public static class TboxNotFoundProjectionException extends RuntimeException {
        public TboxNotFoundProjectionException(String deviceSn, String hsmUid) {
            super("TSP 资产未导入或关联缺失 sn=" + deviceSn + ", hsmUid=" + hsmUid);
        }
    }
}
