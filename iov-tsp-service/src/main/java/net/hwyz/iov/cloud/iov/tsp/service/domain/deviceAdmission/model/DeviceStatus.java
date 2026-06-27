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

    /** 活跃 **/
    ACTIVE(1, "活跃"),
    /** 未激活 **/
    INACTIVE(2, "未激活"),
    /** 暂停 **/
    SUSPENDED(3, "暂停"),
    /** 停用 **/
    DEACTIVATED(4, "停用");

    private final Integer code;
    private final String desc;

    public static DeviceStatus fromCode(Integer code) {
        for (DeviceStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown DeviceStatus code: " + code);
    }
}
