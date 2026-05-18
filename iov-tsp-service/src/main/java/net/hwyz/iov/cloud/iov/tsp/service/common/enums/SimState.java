package net.hwyz.iov.cloud.iov.tsp.service.common.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

/**
 * SIM卡状态枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum SimState {

    /** 测试 **/
    TEST(1),
    /** 库存 **/
    INVENTORY(2),
    /** 激活 **/
    ACTIVE(3);

    /**
     * 状态码
     */
    public final Integer state;

    public static SimState valOf(Integer val) {
        return Arrays.stream(SimState.values())
                .filter(simState -> Objects.equals(simState.state, val))
                .findFirst()
                .orElse(null);
    }

}
