package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

import lombok.Getter;

/**
 * V2C 聚合成功事件契约校验失败（永久异常，直接进入 DLT）
 * <p>
 * TSP-DSN-CR-012 §8：安全或契约拒绝不可自动重试。错误码用于 DLT header 与指标 label，
 * 不得包含 deviceSn、requestId、keyId 或密码学摘要。
 *
 * @author hwyz_leo
 */
@Getter
public class TboxV2cKeyProvisionedContractException extends RuntimeException {

    /**
     * 稳定错误码（用于 DLT header 与指标 label）
     */
    private final String errorCode;

    public TboxV2cKeyProvisionedContractException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
