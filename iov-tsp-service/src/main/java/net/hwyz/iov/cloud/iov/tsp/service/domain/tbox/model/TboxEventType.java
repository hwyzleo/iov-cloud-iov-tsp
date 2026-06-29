package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * TBOX 事件类型枚举
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum TboxEventType {

    CMD_ACK("CMD_ACK", "指令ACK"),
    FIND_VEHICLE("FIND_VEHICLE", "寻车响应"),
    DEVICE_ONLINE("DEVICE_ONLINE", "设备上线");

    private final String code;
    private final String desc;

    public static TboxEventType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (TboxEventType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown TboxEventType code: " + code);
    }
}
