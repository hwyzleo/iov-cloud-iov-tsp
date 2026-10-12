package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

import lombok.Getter;

/**
 * DEVICE_ONLINE 契约校验失败（永久异常，直接进入 DLT）
 *
 * @author hwyz_leo
 */
@Getter
public class DeviceOnlineContractException extends RuntimeException {

    /**
     * 稳定错误码（用于 DLT header 与指标 label）
     */
    private final String errorCode;

    public DeviceOnlineContractException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
