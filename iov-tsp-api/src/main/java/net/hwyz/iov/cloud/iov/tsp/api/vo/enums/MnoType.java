package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 通讯运营商类型枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum MnoType {

    /** 中国移动 **/
    CMCC,
    /** 中国联通 **/
    CUCC,
    /** 中国电信 **/
    CTCC;

    public static MnoType valOf(String val) {
        return Arrays.stream(MnoType.values())
                .filter(mnoType -> mnoType.name().equals(val))
                .findFirst()
                .orElse(null);
    }

}
