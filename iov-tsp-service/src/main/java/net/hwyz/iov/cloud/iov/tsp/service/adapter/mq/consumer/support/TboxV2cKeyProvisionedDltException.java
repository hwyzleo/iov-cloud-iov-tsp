package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

/**
 * V2C 聚合成功事件 DLT 发布失败异常
 * <p>
 * 调用方据此不提交源 offset，交由 Kafka 重投/恢复。
 *
 * @author hwyz_leo
 */
public class TboxV2cKeyProvisionedDltException extends RuntimeException {

    /**
     * 稳定错误码
     */
    private final String errorCode;

    public TboxV2cKeyProvisionedDltException(String message, String errorCode) {
        super(message + ":" + errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
