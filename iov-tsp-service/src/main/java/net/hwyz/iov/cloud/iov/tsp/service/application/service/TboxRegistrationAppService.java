package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.api.vo.event.TboxV2cKeyProvisionedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.command.RecordTboxOnlineCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.DeviceOnlineOutcome;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.TboxV2cActivationOutcome;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.support.TboxEventDigest;
import net.hwyz.iov.cloud.iov.tsp.service.common.util.DeviceUidNormalizer;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxRegistrationEventRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleTboxProjectionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.ActivationOutcome;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxRegistrationEventRecord;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.service.DeviceRegistrationDomainService;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * TBOX 注册激活应用服务
 * <p>
 * TSP-DSN-CR-008：编排设备登录记录与 V2C 业务激活完成。
 * TSP-DSN-CR-010：
 * <ul>
 *   <li>DEVICE_ONLINE 应用层校验（TBOX 身份唯一、tboxSn、active 绑定、VIN 一致性）与契约摘要冲突检测；</li>
 *   <li>本服务是 {@code tb_tbox_registration_event} 的唯一写入者（R4），领域服务只更新 {@code tb_tbox}。</li>
 * </ul>
 * TSP-DSN-CR-012：V2C 聚合成功事件在本服务内完成 Inbox 占位、生命周期收口与 Inbox 成功事实的同事务写入，
 * 并将结果细分为“成功 / 有界重试 / 永久拒绝”。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TboxRegistrationAppService {

    private final DeviceRegistrationDomainService deviceRegistrationDomainService;
    private final TboxMapper tboxMapper;
    private final VehicleTboxProjectionRepository vehicleTboxProjectionRepository;
    private final TboxRegistrationEventRepository tboxRegistrationEventRepository;

    /**
     * 处理设备上线命令：校验身份/绑定后记录首次/最近登录时间并驱动 PRE_ACTIVE -> ACTIVATING。
     *
     * @param cmd 内部上线命令（hsmUid 已规范化）
     * @return 处理结果
     */
    @Transactional
    public DeviceOnlineOutcome handleOnline(RecordTboxOnlineCmd cmd) {
        if (cmd == null) {
            log.warn("收到空的 DEVICE_ONLINE 命令，忽略");
            return DeviceOnlineOutcome.PROCESSED;
        }
        String hsmUid = DeviceUidNormalizer.normalize(cmd.hsmUid());
        if (!DeviceUidNormalizer.isValid(hsmUid)) {
            log.warn("DEVICE_ONLINE 非法身份 hsmUid 摘要[{}]，隔离", mask(hsmUid));
            return DeviceOnlineOutcome.PERMANENT_TBOX_IDENTITY_CONFLICT;
        }

        String digest = TboxEventDigest.deviceOnline(cmd);
        Optional<TboxRegistrationEventRecord> existing =
                tboxRegistrationEventRepository.findByEventId(cmd.eventId());
        if (existing.isPresent()) {
            TboxRegistrationEventRecord record = existing.get();
            String existingDigest = record.contractDigest();
            if (StrUtil.isBlank(existingDigest) || existingDigest.equals(digest)) {
                log.debug("DEVICE_ONLINE 事件[{}]已消费，幂等成功", cmd.eventId());
                return DeviceOnlineOutcome.DUPLICATE;
            }
            log.warn("DEVICE_ONLINE 事件契约冲突: eventId={}, 已存摘要与当前不一致，拒绝并告警", cmd.eventId());
            return DeviceOnlineOutcome.PERMANENT_CONTRACT_CONFLICT;
        }

        List<TboxPo> tbList = tboxMapper.selectAllByHsm(hsmUid);
        if (tbList == null || tbList.isEmpty()) {
            log.warn("DEVICE_ONLINE 设备不存在, hsmUid摘要[{}], eventId={}，进入对账重试", mask(hsmUid), cmd.eventId());
            return DeviceOnlineOutcome.RETRY_TBOX_NOT_FOUND;
        }
        if (tbList.size() > 1) {
            log.warn("DEVICE_ONLINE HSM 身份不唯一, hsmUid摘要[{}], 命中{}条, eventId={}，隔离",
                    mask(hsmUid), tbList.size(), cmd.eventId());
            return DeviceOnlineOutcome.PERMANENT_TBOX_IDENTITY_CONFLICT;
        }
        TboxPo tbox = tbList.get(0);

        if (StrUtil.isNotBlank(cmd.tboxSn()) && !cmd.tboxSn().equals(tbox.getSn())) {
            log.warn("DEVICE_ONLINE tboxSn 与本地设备不一致, eventId={}，拒绝", cmd.eventId());
            return DeviceOnlineOutcome.PERMANENT_TBOX_SN_MISMATCH;
        }

        List<VehicleTbox> activeBindings =
                vehicleTboxProjectionRepository.listActiveTboxBindingsBySn(tbox.getSn());
        if (activeBindings == null || activeBindings.isEmpty()) {
            log.warn("DEVICE_ONLINE 无 active 绑定投影, deviceSn={}, eventId={}，进入对账重试", tbox.getSn(), cmd.eventId());
            return DeviceOnlineOutcome.RETRY_UNBOUND;
        }
        if (activeBindings.size() > 1) {
            log.warn("DEVICE_ONLINE active 绑定冲突, deviceSn={}, 命中{}条, eventId={}，隔离",
                    tbox.getSn(), activeBindings.size(), cmd.eventId());
            return DeviceOnlineOutcome.PERMANENT_BINDING_CONFLICT;
        }
        VehicleTbox binding = activeBindings.get(0);
        if (StrUtil.isNotBlank(cmd.vin()) && !cmd.vin().equals(binding.getVin())) {
            log.warn("DEVICE_ONLINE event.vin 与当前 active VIN 不一致, deviceSn={}, eventId={}，拒绝且不回写投影",
                    tbox.getSn(), cmd.eventId());
            return DeviceOnlineOutcome.PERMANENT_VIN_MISMATCH;
        }

        int updated = deviceRegistrationDomainService.recordSuccessfulLogin(
                tbox.getId(), tbox.getSn(), cmd.eventId(), cmd.occurredAt(), cmd.connectionGeneration());
        if (updated == 0) {
            log.warn("DEVICE_ONLINE 未推进状态（BLOCKED/RETIRED），保留审计, deviceSn={}, eventId={}",
                    tbox.getSn(), cmd.eventId());
        }
        saveEvent(new TboxRegistrationEventRecord(
                cmd.eventId(), null, tbox.getSn(),
                DeviceRegistrationDomainService.EVENT_TYPE_DEVICE_ONLINE,
                cmd.occurredAt(), LocalDateTime.now(), "SUCCEEDED", digest,
                cmd.sourceTopic(), cmd.connectionGeneration()));
        return DeviceOnlineOutcome.PROCESSED;
    }

    /**
     * 处理 VMD V2C 业务密钥开通结果事件：驱动 ACTIVATING -> ACTIVE，并由本服务写入事件事实。
     * <p>
     * TSP-DSN-CR-012 §5/§6：Inbox 占位（eventId、eventType+requestId）→ 领域校验与状态机 →
     * 原子生命周期更新 + Inbox 成功事实同事务提交；生命周期未成功推进时不落库 Inbox 成功事实。
     *
     * @param event VMD V2C 密钥开通结果事件
     * @return 细分处理结果（供消费者区分 ack / 重试 / DLT）
     */
    @Transactional
    public TboxV2cActivationOutcome handleV2cProvisioned(TboxV2cKeyProvisionedEvent event) {
        if (event == null) {
            log.warn("收到空的 V2C 聚合完成事件，忽略");
            return TboxV2cActivationOutcome.PERMANENT_CONTRACT_INVALID;
        }
        String eventId = event.getEventId();
        String requestId = event.getRequestId();
        if (StrUtil.isBlank(eventId) || StrUtil.isBlank(requestId) || StrUtil.isBlank(event.getDeviceSn())) {
            log.warn("V2C 聚合完成事件缺失必要身份字段，永久拒绝");
            return TboxV2cActivationOutcome.PERMANENT_CONTRACT_INVALID;
        }
        String digest = TboxEventDigest.v2c(event);

        Optional<TboxRegistrationEventRecord> byEventId = tboxRegistrationEventRepository.findByEventId(eventId);
        if (byEventId.isPresent()) {
            if (isDigestConflict(byEventId.get().contractDigest(), digest)) {
                log.warn("V2C 聚合完成事件契约冲突: eventId 已存在且摘要不一致，拒绝并告警");
                return TboxV2cActivationOutcome.PERMANENT_CONTRACT_CONFLICT;
            }
            log.info("V2C 聚合完成事件[{}]已消费，幂等成功", eventId);
            return TboxV2cActivationOutcome.DUPLICATE;
        }
        Optional<TboxRegistrationEventRecord> byRequest = tboxRegistrationEventRepository
                .findByEventTypeAndRequestId(DeviceRegistrationDomainService.EVENT_TYPE_V2C_KEY_PROVISIONED, requestId);
        if (byRequest.isPresent()) {
            if (isDigestConflict(byRequest.get().contractDigest(), digest)) {
                log.warn("V2C 聚合完成事件契约冲突: requestId 已存在且摘要不一致，拒绝并告警");
                return TboxV2cActivationOutcome.PERMANENT_CONTRACT_CONFLICT;
            }
            log.info("V2C 聚合完成事件 requestId 已消费，幂等成功");
            return TboxV2cActivationOutcome.DUPLICATE;
        }

        ActivationOutcome outcome = deviceRegistrationDomainService.completeActivation(event);
        TboxV2cActivationOutcome result = map(outcome);
        if (result.isSuccess()) {
            saveEvent(new TboxRegistrationEventRecord(
                    eventId, requestId, event.getDeviceSn(),
                    DeviceRegistrationDomainService.EVENT_TYPE_V2C_KEY_PROVISIONED,
                    event.getOccurredAt(), LocalDateTime.now(), "SUCCEEDED", digest, null, null));
        }
        return result;
    }

    private TboxV2cActivationOutcome map(ActivationOutcome outcome) {
        return switch (outcome) {
            case ACTIVATED -> TboxV2cActivationOutcome.ACTIVATED;
            case IDEMPOTENT_ACTIVE -> TboxV2cActivationOutcome.IDEMPOTENT_ACTIVE;
            case RETRY_OUT_OF_ORDER -> TboxV2cActivationOutcome.RETRY_OUT_OF_ORDER;
            case RETRY_DEVICE_NOT_FOUND -> TboxV2cActivationOutcome.RETRY_DEVICE_NOT_FOUND;
            case RETRY_BINDING_MISSING -> TboxV2cActivationOutcome.RETRY_BINDING_MISSING;
            case RETRY_TRANSIENT -> TboxV2cActivationOutcome.RETRY_TRANSIENT;
            case REJECTED_CONTRACT -> TboxV2cActivationOutcome.PERMANENT_CONTRACT_INVALID;
            case REJECTED_DEVICE_STATE -> TboxV2cActivationOutcome.PERMANENT_DEVICE_STATE;
            case REJECTED_BINDING_CONFLICT -> TboxV2cActivationOutcome.PERMANENT_BINDING_CONFLICT;
        };
    }

    private boolean isDigestConflict(String existingDigest, String currentDigest) {
        return StrUtil.isNotBlank(existingDigest) && !existingDigest.equals(currentDigest);
    }

    private void saveEvent(TboxRegistrationEventRecord record) {
        tboxRegistrationEventRepository.save(record);
    }

    private String mask(String hsmUid) {
        return net.hwyz.iov.cloud.iov.tsp.service.common.util.SensitiveDataMasker.maskHsmUid(hsmUid);
    }
}
