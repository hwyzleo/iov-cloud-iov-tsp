package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.UserVehicleAuditRecord;

/**
 * 用户车辆关系业务审计仓储。
 *
 * @author hwyz_leo
 */
public interface UserVehicleAuditRepository {

    /**
     * 新增审计记录
     *
     * @param record 审计记录
     */
    void insert(UserVehicleAuditRecord record);
}
