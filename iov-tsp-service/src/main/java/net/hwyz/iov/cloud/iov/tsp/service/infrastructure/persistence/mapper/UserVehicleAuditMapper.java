package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.UserVehicleAuditPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * 用户车辆关系业务审计 DAO。
 *
 * @author hwyz_leo
 */
@Mapper
public interface UserVehicleAuditMapper {

    /**
     * 新增审计记录
     */
    @Insert("INSERT INTO tb_user_vehicle_audit (audit_id, biz_type, biz_id, vin, account_id, from_state, "
            + "to_state, operator_source, reason, fact_ref, occurred_at) "
            + "VALUES (#{auditId}, #{bizType}, #{bizId}, #{vin}, #{accountId}, #{fromState}, #{toState}, "
            + "#{operatorSource}, #{reason}, #{factRef}, #{occurredAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserVehicleAuditPo po);
}
