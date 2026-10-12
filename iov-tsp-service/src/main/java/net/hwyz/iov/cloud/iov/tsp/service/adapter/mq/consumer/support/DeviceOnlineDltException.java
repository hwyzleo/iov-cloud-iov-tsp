package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

/**
 * DLT 发布失败异常
 * <p>
 * 调用方据此不提交源 offset，交由 Kafka 重投/恢复。
 *
 * @author hwyz_leo
 */
public class DeviceOnlineDltException extends RuntimeException {

    /**
     * 稳定错误码
     */
    private final String errorCode;

    public DeviceOnlineDltException(String message, String errorCode) {
        super(message + ":" + errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
