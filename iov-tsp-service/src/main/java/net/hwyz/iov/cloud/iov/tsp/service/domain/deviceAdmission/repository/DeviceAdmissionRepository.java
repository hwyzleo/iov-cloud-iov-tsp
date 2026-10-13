package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 设备接入准入仓储接口（TSP-DSN-CR-011 §3）
 *
 * @author hwyz_leo
 */
public interface DeviceAdmissionRepository {

    /**
     * 根据规范化 HSM UID 查询 TBOX 设备事实
     *
     * @param hsm 规范化 HSM UID
     * @return TBOX 信息；不存在返回空
     */
    Optional<TboxInfo> findByHsm(String hsm);

    /**
     * 查询设备（SN）当前全部 ACTIVE 车辆绑定投影（不 LIMIT，便于检测绑定冲突）
     *
     * @param sn 设备序列号
     * @return ACTIVE 绑定列表
     */
    List<BindingInfo> findActiveBindingsBySn(String sn);

    /**
     * TBOX 设备事实值对象
     *
     * @param id           主键
     * @param sn           序列号
     * @param hsm          规范化 HSM UID
     * @param deviceStatus 设备生命周期状态码
     * @param modifyTime   最近更新时间（用于 updatedAt）
     */
    record TboxInfo(
            Long id,
            String sn,
            String hsm,
            Integer deviceStatus,
            Instant modifyTime
    ) {
    }

    /**
     * 车辆-设备绑定投影值对象
     *
     * @param vin            车辆识别码
     * @param bindingVersion 绑定版本
     * @param lastEventTime  绑定投影最近事件时间（用于 updatedAt）
     */
    record BindingInfo(
            String vin,
            Long bindingVersion,
            Instant lastEventTime
    ) {
    }
}
