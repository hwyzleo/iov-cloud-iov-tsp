package net.hwyz.iov.cloud.iov.tsp.service.domain.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

@AllArgsConstructor
public enum SimState {

    TEST(1),
    INVENTORY(2),
    ACTIVE(3);

    public final Integer state;

    public static SimState valOf(Integer val) {
        return Arrays.stream(SimState.values())
                .filter(simState -> Objects.equals(simState.state, val))
                .findFirst()
                .orElse(null);
    }

}