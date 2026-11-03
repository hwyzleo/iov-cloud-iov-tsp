package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Tbox;

import java.time.LocalDateTime;

public interface TboxRepository {

    Tbox getBySn(String sn);

    int save(Tbox tbox);

    /**
     * VIAM 投影：按 hsmUid 查询设备（投影链资产关联；不存在返回 null）
     */
    Tbox getByHsm(String hsm);

    /**
     * VIAM 投影：最近登录事实单调更新（唯一业务写入者=VIAM 投影处理链）
     */
    void applyViamLoginFact(Long id, LocalDateTime ts, LocalDateTime projectedAt, String syncStatus);

    /**
     * VIAM 投影：应用安全开通快照（device_status/first/last_login_time + 元数据；不写 activate_time）
     */
    void applyViamSnapshot(Long id, Integer deviceStatus, LocalDateTime firstLoginTime,
                           LocalDateTime lastLoginTime, Long securityVersion,
                           LocalDateTime projectedAt, String syncStatus);
}