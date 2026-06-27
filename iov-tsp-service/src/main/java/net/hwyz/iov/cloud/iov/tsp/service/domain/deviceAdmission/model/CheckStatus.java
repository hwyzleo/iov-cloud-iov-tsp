package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 检查状态枚举
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum CheckStatus {

    /** 检查通过 **/
    PASS("PASS", "检查通过"),
    /** 检查失败 **/
    FAIL("FAIL", "检查失败"),
    /** 检查错误 **/
    ERROR("ERROR", "检查错误");

    private final String code;
    private final String desc;
}
