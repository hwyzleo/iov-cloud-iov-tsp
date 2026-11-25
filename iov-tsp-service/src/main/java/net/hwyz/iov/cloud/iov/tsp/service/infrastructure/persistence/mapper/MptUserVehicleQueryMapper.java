package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.UserVehicleHistoryPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MPT 用户车辆关系专用只读查询 DAO（IOV-TSP-DSN-CR-021 D021-02）。
 * <p>
 * 过滤与分页全部在数据库侧完成；撤销状态通过解绑台账左连接一次取出，
 * 不在应用层逐行查询或内存分页。
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptUserVehicleQueryMapper {

    /**
     * 按可选条件分页查询关系历史（PageHelper 在执行前注入分页）
     *
     * @param vin         车辆 VIN（可空）
     * @param relationId  关系ID（可空）
     * @param operationId 解绑操作ID（可空）
     * @return 历史行
     */
    @Select("<script>"
            + "SELECT r.relation_id AS relationId, r.vin, r.account_id AS accountId, r.role, r.state, "
            + "r.bound_at AS boundAt, r.released_at AS releasedAt, r.release_reason AS releaseReason, "
            + "r.release_operation_id AS releaseOperationId, r.released_by AS releasedBy, "
            + "o.credential_revocation_status AS credentialRevocationStatus "
            + "FROM tb_user_vehicle_relation r "
            + "LEFT JOIN tb_user_vehicle_unbind_operation o "
            + "ON o.operation_id = r.release_operation_id AND o.row_valid = 1 "
            + "WHERE r.row_valid = 1 "
            + "<if test='vin != null and vin != \"\"'>AND r.vin = #{vin} </if>"
            + "<if test='relationId != null and relationId != \"\"'>AND r.relation_id = #{relationId} </if>"
            + "<if test='operationId != null and operationId != \"\"'>"
            + "AND r.release_operation_id = #{operationId} </if>"
            + "ORDER BY r.bound_at DESC, r.id DESC"
            + "</script>")
    List<UserVehicleHistoryPo> selectHistory(@Param("vin") String vin,
                                             @Param("relationId") String relationId,
                                             @Param("operationId") String operationId);
}
