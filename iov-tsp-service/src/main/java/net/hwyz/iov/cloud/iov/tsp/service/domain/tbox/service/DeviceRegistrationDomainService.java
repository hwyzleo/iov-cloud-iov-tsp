package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.api.vo.event.TboxV2cKeyProvisionedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxRegistrationEventRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleTboxProjectionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxRegistrationEventRecord;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 设备注册激活领域服务
 * <p>
 * TSP-DSN-CR-008（2026-10-12 修订）：TBOX 两阶段注册激活
 * 1. 接入成功：记录首次/最近登录时间，PRE_ACTIVE -> ACTIVATING；
 * 2. 业务激活完成：消费 VMD 聚合完成事件 vmd.tbox-v2c-key-provisioned，校验完整用途集与状态后
 *    条件更新 ACTIVATING -> ACTIVE，set-once 写入 activate_time。
 * <p>
 * TSP 不持有密钥目录或密钥材料，不自动推进车辆生命周期；消费幂等记录不保存 keyId 列表。
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
    private final TboxRegistrationEventRepository tboxRegistrationEventRepository;

    /**
     * 记录成功登录（幂等）
     * first_login_time set-once；last_login_time 取 max(existing, event.ts)；
     * PRE_ACTIVE -> ACTIVATING；ACTIVE 仅刷新 last_login_time；BLOCKED/RETIRED 不迁移。
     * 以 eventId 去重，按事件时间容忍乱序。
     *
     * @param eventId 事件ID，用于幂等去重
     * @param hsmUid  规范设备身份（tb_tbox.hsm）
     * @param tboxSn  设备序列号（仅用于日志与消费记录追溯）
     * @param ts      事件时间，为空时取当前时间
     */
    @Transactional
    public void recordSuccessfulLogin(String eventId, String hsmUid, String tboxSn, LocalDateTime ts) {
        if (StrUtil.isBlank(hsmUid)) {
            log.warn("DEVICE_ONLINE 缺失规范设备身份 hsmUid，tboxSn[{}]，忽略登录记录", tboxSn);
            return;
        }
        if (StrUtil.isNotBlank(eventId) && tboxRegistrationEventRepository.findByEventId(eventId).isPresent()) {
            log.debug("DEVICE_ONLINE 事件[{}]已消费，幂等跳过", eventId);
            return;
        }

        LocalDateTime eventTime = ts != null ? ts : LocalDateTime.now();
        int updated = tboxMapper.recordLogin(hsmUid, eventTime,
                DeviceStatus.PRE_ACTIVE.getCode(),
                DeviceStatus.ACTIVATING.getCode(),
                DeviceStatus.ACTIVE.getCode());

        if (updated > 0) {
            if (StrUtil.isNotBlank(eventId)) {
                tboxRegistrationEventRepository.save(new TboxRegistrationEventRecord(
                        eventId, null, tboxSn, EVENT_TYPE_DEVICE_ONLINE,
                        toInstant(eventTime),
                        LocalDateTime.now(), "SUCCEEDED", null));
            }
            log.info("设备[{}]登录记录成功, tboxSn={}, 事件时间={}", hsmUid, tboxSn, eventTime);
        } else {
            log.warn("设备[{}]登录记录未更新(设备不存在或处于 BLOCKED/RETIRED)，tboxSn={}", hsmUid, tboxSn);
        }
    }

    /**
     * 完成业务激活（幂等）
     * <p>
     * 校验事件契约、消费幂等与设备/唯一 active 绑定投影/当前状态后，
     * 条件更新 ACTIVATING -> ACTIVE 并 set-once 写入 activate_time；否则 fail-closed。
     *
     * @param event VMD V2C 聚合完成事件
     * @return 是否已成功完成（含幂等已处理）
     */
    @Transactional
    public boolean completeActivation(TboxV2cKeyProvisionedEvent event) {
        if (event == null) {
            log.warn("收到空的 V2C 聚合完成事件，忽略");
            return false;
        }
        if (!isContractValid(event)) {
            log.warn("V2C 聚合完成事件契约校验失败, eventId={}, requestId={}, deviceSn={}, domain={}, result={}",
                    event.getEventId(), event.getRequestId(), event.getDeviceSn(),
                    event.getBusinessDomain(), event.getResult());
            return false;
        }

        String eventId = event.getEventId();
        String requestId = event.getRequestId();
        String deviceSn = event.getDeviceSn();
        String digest = computeContractDigest(event);

        // 幂等：eventId 优先，其次 (eventType, requestId)
        if (tboxRegistrationEventRepository.findByEventId(eventId).isPresent()) {
            log.info("V2C 聚合完成事件[{}]已消费，幂等成功", eventId);
            return true;
        }
        Optional<TboxRegistrationEventRecord> existingByRequest =
                tboxRegistrationEventRepository.findByEventTypeAndRequestId(EVENT_TYPE_V2C_KEY_PROVISIONED, requestId);
        if (existingByRequest.isPresent()) {
            String existingDigest = existingByRequest.get().contractDigest();
            if (existingDigest != null && !existingDigest.equals(digest)) {
                log.warn("V2C 聚合完成事件契约冲突: requestId={}, 已存摘要与当前不一致，拒绝并告警", requestId);
                return false;
            }
            log.info("V2C 聚合完成事件 requestId={} 已消费，幂等成功", requestId);
            return true;
        }

        // 设备存在性
        TboxPo tbox = tboxMapper.selectBySn(deviceSn);
        if (tbox == null) {
            log.warn("V2C 聚合完成事件设备不存在, deviceSn={}, requestId={}，fail-closed 并告警", deviceSn, requestId);
            return false;
        }

        // 唯一当前 active 绑定投影
        List<VehicleTbox> activeBindings = vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(deviceSn);
        if (activeBindings.size() != 1) {
            log.warn("V2C 聚合完成事件绑定投影缺失/冲突, deviceSn={}, activeBindings={}, requestId={}，fail-closed 并告警",
                    deviceSn, activeBindings.size(), requestId);
            return false;
        }

        DeviceStatus status = resolveStatus(tbox.getDeviceStatus());
        if (status == null) {
            log.warn("V2C 聚合完成事件设备状态非法, deviceSn={}, deviceStatus={}，fail-closed", deviceSn, tbox.getDeviceStatus());
            return false;
        }
        if (status == DeviceStatus.BLOCKED || status == DeviceStatus.RETIRED) {
            log.warn("V2C 聚合完成事件设备状态[{}]不允许激活, deviceSn={}, requestId={}，fail-closed 并告警",
                    status, deviceSn, requestId);
            return false;
        }
        if (status == DeviceStatus.PRE_ACTIVE) {
            log.warn("V2C 聚合完成事件早于 DEVICE_ONLINE（跨 Topic 乱序）, deviceSn={}, requestId={}，进入重试/对账",
                    deviceSn, requestId);
            return false;
        }
        if (status == DeviceStatus.ACTIVE) {
            recordConsumedEvent(event, digest, "SUCCEEDED");
            log.info("设备[{}]已为 ACTIVE，V2C 聚合完成事件幂等成功, requestId={}", deviceSn, requestId);
            return true;
        }

        // ACTIVATING -> ACTIVE（条件更新保证并发幂等）
        LocalDateTime activateTime = event.getOccurredAt() != null
                ? LocalDateTime.ofInstant(event.getOccurredAt(), ZoneId.systemDefault())
                : LocalDateTime.now();
        int updated = tboxMapper.completeActivation(deviceSn, activateTime,
                DeviceStatus.ACTIVATING.getCode(),
                DeviceStatus.ACTIVE.getCode());
        recordConsumedEvent(event, digest, updated > 0 ? "SUCCEEDED" : "SKIPPED");
        if (updated > 0) {
            log.info("设备[{}]业务激活完成, activateTime={}, requestId={}", deviceSn, activateTime, requestId);
        } else {
            log.info("设备[{}]业务激活条件更新未命中（并发已激活）, requestId={}，幂等成功", deviceSn, requestId);
        }
        return true;
    }

    private boolean isContractValid(TboxV2cKeyProvisionedEvent event) {
        if (StrUtil.isBlank(event.getEventId())
                || StrUtil.isBlank(event.getRequestId())
                || StrUtil.isBlank(event.getDeviceSn())) {
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
            if (key == null || StrUtil.isBlank(key.getPurpose()) || StrUtil.isBlank(key.getKeyId())
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

    private void recordConsumedEvent(TboxV2cKeyProvisionedEvent event, String digest, String result) {
        tboxRegistrationEventRepository.save(new TboxRegistrationEventRecord(
                event.getEventId(), event.getRequestId(), event.getDeviceSn(),
                EVENT_TYPE_V2C_KEY_PROVISIONED, event.getOccurredAt(), LocalDateTime.now(), result, digest));
    }

    /**
     * 事件契约不可逆摘要（不含 keyId）：deviceSn|domain|purpose:version(排序)
     */
    private String computeContractDigest(TboxV2cKeyProvisionedEvent event) {
        List<String> parts = event.getKeys().stream()
                .map(key -> key.getPurpose() + ":" + key.getBusinessKeyVersion())
                .sorted()
                .toList();
        String raw = event.getDeviceSn() + "|" + event.getBusinessDomain() + "|" + String.join(",", parts);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            log.warn("计算事件契约摘要失败", e);
            return null;
        }
    }

    private Instant toInstant(LocalDateTime time) {
        return time != null ? time.atZone(ZoneId.systemDefault()).toInstant() : null;
    }
}
