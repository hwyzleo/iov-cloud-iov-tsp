package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 远控类型枚举类
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum RemoteControlType {

    /** 寻车 **/
    FIND_VEHICLE(3, 30);

    /**
     * 重试限制
     */
    private final Integer retryLimit;
    /**
     * 超时时间
     */
    private final Integer timeout;

}
