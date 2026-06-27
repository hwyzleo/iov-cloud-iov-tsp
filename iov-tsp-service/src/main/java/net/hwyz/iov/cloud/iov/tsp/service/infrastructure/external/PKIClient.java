package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.external;

/**
 * PKI服务客户端接口
 *
 * @author hwyz_leo
 */
public interface PKIClient {

    /**
     * 检查证书是否已被吊销
     * @param hsm 设备HSM标识
     * @return true-已吊销, false-未吊销
     */
    boolean checkRevocation(String hsm);
}
