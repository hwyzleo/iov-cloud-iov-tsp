package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.api.vo.event.TboxV2cKeyProvisionedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleTboxProjectionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.ActivationOutcome;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 设备注册激活领域服务
 * <p>
 * TSP-DSN-CR-008：TBOX 两阶段注册激活
 * 1. 接入成功：记录首次/最近登录时间，PRE_ACTIVE -> ACTIVATING；
 * 2. 业务激活完成：ACTIVATING -> ACTIVE，set-once 写入 activate_time。
 * <p>
 * TSP-DSN-CR-010 R4：本领域服务只更新 {@code tb_tbox}，按已解析主键条件更新（不再使用非唯一
 * {@code WHERE hsm=?}）；注册激活事件事实（{@code tb_tbox_registration_event}）由
 * {@code TboxRegistrationAppService} 作为唯一写入者落库，领域服务仅返回 {@link ActivationOutcome}。
 * <p>
 * TSP-DSN-CR-012：ACTIVATING→ACTIVE 按已解析主键条件更新，set-once activateTime；
 * 条件未命中时同事务重读，生命周期未推进不返回成功（避免 Inbox 成功事实与设备状态不一致）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceRegistrationDomainService {

    /**
     * V2C 聚合完成事件类型（消费幂等记录用）
     */
    public static final String EVENT_TYPE_V2C_KEY_PROVISIONED = "TBOX_V2C_KEY_PROVISIONED";

    /**
     * 设备上线事件类型（消费幂等记录用）
     */
    public static final String EVENT_TYPE_DEVICE_ONLINE = "DEVICE_ONLINE";

    private static final Set<String> V2C_COMPLETE_PURPOSES = Set.of(
            TboxV2cKeyProvisionedEvent.PURPOSE_UPLINK_AEAD,
            TboxV2cKeyProvisionedEvent.PURPOSE_DOWNLINK_AEAD);

    private final TboxMapper tboxMapper;
    private final VehicleTboxProjectionRepository vehicleTboxProjectionRepository;

    /**
     * 记录成功登录（幂等，按已解析主键更新）
     * first_login_time set-once；last_login_time 取 max(existing, occurredAt)；
     * PRE_ACTIVE -> ACTIVATING；ACTIVE 仅刷新 last_login_time；BLOCKED/RETIRED 不迁移。
     * <p>
     * 事件事实落库由应用服务负责；deviceSn/eventId/connectionGeneration 仅用于审计日志。
     *
     * @param tboxId               已解析的 TBOX 主键
     * @param deviceSn             TBOX 序列号（审计）
     * @param eventId              事件ID（审计）
     * @param occurredAt           事件时间
     * @param connectionGeneration VAGW 连接代际（审计）
     * @return 更新行数（0 表示设备不存在或处于 BLOCKED/RETIRED）
     */
    @Transactional
    public int recordSuccessfulLogin(Long tboxId, String deviceSn, String eventId,
                                     Instant occurredAt, long connectionGeneration) {
        if (tboxId == null) {
            log.warn("DEVICE_ONLINE 缺失已解析 TBOX 主键，deviceSn[{}]，忽略登录记录", deviceSn);
            return 0;
        }
        LocalDateTime eventTime = occurredAt != null
                ? LocalDateTime.ofInstant(occurredAt, ZoneId.systemDefault())
                : LocalDateTime.now();
        log.debug("处理设备上线事件, tboxId={}, deviceSn={}, eventId={}, generation={}, occurredAt={}",
                tboxId, deviceSn, eventId, connectionGeneration, eventTime);

        int updated = tboxMapper.recordLoginById(tboxId, eventTime,
                DeviceStatus.PRE_ACTIVE.getCode(),
                DeviceStatus.ACTIVATING.getCode(),
                DeviceStatus.ACTIVE.getCode());

        if (updated > 0) {
            log.info("设备[{}]登录记录成功, eventId={}, 事件时间={}", deviceSn, eventId, eventTime);
        } else {
            log.warn("设备[{}]登录记录未更新(设备不存在或处于 BLOCKED/RETIRED), eventId={}", deviceSn, eventId);
        }
        return updated;
    }

    /**
     * 完成业务激活（幂等，只更新 tb_tbox）
     * <p>
     * TSP-DSN-CR-012 §6：校验事件契约、唯一 active 绑定投影与当前状态后，按已解析主键
     * 条件更新 ACTIVATING -> ACTIVE 并 set-once 写入 activate_time；事件事实由应用服务在同一事务落库。
     * 条件更新未命中时同事务重读设备状态：已 ACTIVE 视为幂等成功，否则返回可重试结果，
     * 禁止在生命周期未推进时落库 Inbox 成功事实。
     *
     * @param event VMD V2C 聚合完成事件
     * @return 处理结果
     */
    @Transactional
    public ActivationOutcome completeActivation(TboxV2cKeyProvisionedEvent event) {
        if (event == null) {
            log.warn("收到空的 V2C 聚合完成事件，忽略");
            return ActivationOutcome.REJECTED_CONTRACT;
        }
        if (!isContractValid(event)) {
            log.warn("V2C 聚合完成事件契约校验失败, eventId={}, requestId={}, deviceSn={}, domain={}, result={}, contractVersion={}",
                    event.getEventId(), event.getRequestId(), event.getDeviceSn(),
                    event.getBusinessDomain(), event.getResult(), event.getContractVersion());
            return ActivationOutcome.REJECTED_CONTRACT;
        }

        String deviceSn = event.getDeviceSn();
        String requestId = event.getRequestId();

        // 设备存在性（跨 Topic 乱序/导入延迟：有界重试后人工对账）
        TboxPo tbox = tboxMapper.selectBySn(deviceSn);
        if (tbox == null) {
            log.warn("V2C 聚合完成事件设备不存在, deviceSn={}, requestId={}，进入有界重试/对账", deviceSn, requestId);
            return ActivationOutcome.RETRY_DEVICE_NOT_FOUND;
        }

        // 唯一当前 active 绑定投影
        List<VehicleTbox> activeBindings = vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(deviceSn);
        if (activeBindings == null || activeBindings.isEmpty()) {
            log.warn("V2C 聚合完成事件绑定投影缺失, deviceSn={}, requestId={}，进入有界重试/对账", deviceSn, requestId);
            return ActivationOutcome.RETRY_BINDING_MISSING;
        }
        if (activeBindings.size() > 1) {
            log.warn("V2C 聚合完成事件绑定投影冲突, deviceSn={}, activeBindings={}, requestId={}，永久拒绝并告警",
                    deviceSn, activeBindings.size(), requestId);
            return ActivationOutcome.REJECTED_BINDING_CONFLICT;
        }

        DeviceStatus status = resolveStatus(tbox.getDeviceStatus());
        if (status == null) {
            log.warn("V2C 聚合完成事件设备状态非法, deviceSn={}, deviceStatus={}，fail-closed", deviceSn, tbox.getDeviceStatus());
            return ActivationOutcome.REJECTED_DEVICE_STATE;
        }
        if (status == DeviceStatus.BLOCKED || status == DeviceStatus.RETIRED) {
            log.warn("V2C 聚合完成事件设备状态[{}]不允许激活, deviceSn={}, requestId={}，永久拒绝并告警",
                    status, deviceSn, requestId);
            return ActivationOutcome.REJECTED_DEVICE_STATE;
        }
        if (status == DeviceStatus.PRE_ACTIVE) {
            log.warn("V2C 聚合完成事件早于 DEVICE_ONLINE（跨 Topic 乱序）, deviceSn={}, requestId={}，等待上线事件后重试",
                    deviceSn, requestId);
            return ActivationOutcome.RETRY_OUT_OF_ORDER;
        }
        if (status == DeviceStatus.ACTIVE) {
            log.info("设备[{}]已为 ACTIVE，V2C 聚合完成事件幂等成功, requestId={}", deviceSn, requestId);
            return ActivationOutcome.IDEMPOTENT_ACTIVE;
        }

        // ACTIVATING -> ACTIVE（按已解析主键条件更新，保证并发幂等与 set-once activateTime）
        LocalDateTime activateTime = event.getOccurredAt() != null
                ? LocalDateTime.ofInstant(event.getOccurredAt(), ZoneId.systemDefault())
                : LocalDateTime.now();
        int updated = tboxMapper.completeActivationById(tbox.getId(), activateTime,
                DeviceStatus.ACTIVATING.getCode(),
                DeviceStatus.ACTIVE.getCode());
        if (updated > 0) {
            log.info("设备[{}]业务激活完成, activateTime={}, requestId={}", deviceSn, activateTime, requestId);
            return ActivationOutcome.ACTIVATED;
        }

        // 条件更新未命中：同事务重读，确认是否已被并发激活
        TboxPo latest = tboxMapper.selectBySn(deviceSn);
        if (latest == null) {
            log.warn("设备[{}]激活条件更新未命中且重读不存在, requestId={}，进入有界重试", deviceSn, requestId);
            return ActivationOutcome.RETRY_DEVICE_NOT_FOUND;
        }
        DeviceStatus latestStatus = resolveStatus(latest.getDeviceStatus());
        if (latestStatus == DeviceStatus.ACTIVE) {
            log.info("设备[{}]已被并发激活, requestId={}，幂等成功", deviceSn, requestId);
            return ActivationOutcome.IDEMPOTENT_ACTIVE;
        }
        if (latestStatus == DeviceStatus.BLOCKED || latestStatus == DeviceStatus.RETIRED) {
            log.warn("设备[{}]激活条件更新未命中且状态[{}]不允许激活, requestId={}，永久拒绝并告警",
                    deviceSn, latestStatus, requestId);
            return ActivationOutcome.REJECTED_DEVICE_STATE;
        }
        log.warn("设备[{}]激活条件更新未命中, latestStatus={}, requestId={}，进入有界重试",
                deviceSn, latestStatus, requestId);
        return ActivationOutcome.RETRY_TRANSIENT;
    }

    private boolean isContractValid(TboxV2cKeyProvisionedEvent event) {
        if (StrUtil.isBlank(event.getEventId())
                || StrUtil.isBlank(event.getRequestId())
                || StrUtil.isBlank(event.getDeviceSn())) {
            return false;
        }
        if (StrUtil.isNotBlank(event.getContractVersion())
                && !TboxV2cKeyProvisionedEvent.SUPPORTED_CONTRACT_VERSIONS.contains(event.getContractVersion())) {
            return false;
        }
        if (!TboxV2cKeyProvisionedEvent.BUSINESS_DOMAIN_V2C_COMM.equals(event.getBusinessDomain())) {
            return false;
        }
        if (!TboxV2cKeyProvisionedEvent.RESULT_SUCCEEDED.equalsIgnoreCase(event.getResult())) {
            return false;
        }
        List<TboxV2cKeyProvisionedEvent.Key> keys = event.getKeys();
        if (keys == null || keys.size() != V2C_COMPLETE_PURPOSES.size()) {
            return false;
        }
        Set<String> purposes = new HashSet<>();
        for (TboxV2cKeyProvisionedEvent.Key key : keys) {
            if (key == null
                    || StrUtil.isBlank(key.getPurpose())
                    || StrUtil.isBlank(key.getKeyId())
                    || key.getBusinessKeyVersion() == null || key.getBusinessKeyVersion() <= 0) {
                return false;
            }
            if (!purposes.add(key.getPurpose())) {
                return false;
            }
        }
        return purposes.equals(V2C_COMPLETE_PURPOSES);
    }

    private DeviceStatus resolveStatus(Integer code) {
        try {
            return DeviceStatus.fromCode(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
