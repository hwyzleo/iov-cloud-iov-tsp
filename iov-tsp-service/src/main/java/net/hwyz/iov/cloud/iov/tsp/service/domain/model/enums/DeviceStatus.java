package net.hwyz.iov.cloud.iov.tsp.service.domain.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备安全开通状态枚举（IOV-TSP-DSN-CR-016 迁出后保留）
 * <p>
 * 语义已从「TSP 准入裁决设备状态」迁移为「VIAM 安全开通状态投影码值」：
 * 唯一业务写入者=VIAM 安全投影处理链（快照 state 映射），TSP 仅用于 MPT 展示与
 * 异常标签派生；码值与历史枚举一致（兼容存量展示/筛选）。
 * 运营退役（报废）独立表达，不混入本安全状态枚举。
 * </p>
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum DeviceStatus {

    PRE_ACTIVE(1, "待激活"),
    ACTIVATING(5, "激活中"),
    ACTIVE(2, "在役"),
    RETIRED(3, "安全身份退役"),
    BLOCKED(4, "冻结黑名单");

    private final Integer code;
    private final String desc;

    public static DeviceStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DeviceStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown DeviceStatus code: " + code);
    }
}
