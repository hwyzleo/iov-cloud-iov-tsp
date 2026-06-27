package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备接入裁决结果枚举
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum DeviceAdmissionResult {

    /** 允许接入 **/
    ALLOW("ALLOW", "允许接入"),
    /** 拒绝接入 **/
    DENY("DENY", "拒绝接入");

    private final String code;
    private final String desc;
}
