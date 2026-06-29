package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备状态枚举
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum DeviceStatus {

    PRE_ACTIVE(1, "待激活"),
    ACTIVE(2, "在役"),
    RETIRED(3, "报废"),
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
