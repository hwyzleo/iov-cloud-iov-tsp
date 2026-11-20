package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

/**
 * 绑定操作调用方范围（幂等键作用域）。
 *
 * @author hwyz_leo
 */
public enum BindingCallerScope {

    /**
     * C 端 App
     */
    APP,

    /**
     * 可信车端
     */
    VEHICLE
}
