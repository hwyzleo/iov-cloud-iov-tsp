package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

/**
 * 当前账号在指定 VIN 上的人车关系查询结果状态（IOV-TSP-DSN-CR-018 D018-01）。
 * <p>
 * BOUND/NOT_BOUND 是服务端二值事实；查询失败、认证失败或依赖异常必须走失败契约，
 * 不得映射为 NOT_BOUND。客户端 UNKNOWN 只表示尚未取得权威结果。
 *
 * @author hwyz_leo
 */
public enum UserVehicleRelationStatus {

    /**
     * 当前账号在该 VIN 上存在有效主使用人关系
     */
    BOUND,

    /**
     * 当前账号在该 VIN 上不存在有效关系（不含他人关系信息）
     */
    NOT_BOUND
}
