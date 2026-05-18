package net.hwyz.iov.cloud.iov.tsp.service.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 消息流向枚举类
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum MsgFlow {

    /** 上行 **/
    UP(1),
    /** 下行 **/
    DOWN(2);

    private final int value;

    public static MsgFlow valOf(Integer val) {
        return Arrays.stream(MsgFlow.values())
                .filter(msgFlow -> msgFlow.value == val)
                .findFirst()
                .orElse(null);
    }

}
