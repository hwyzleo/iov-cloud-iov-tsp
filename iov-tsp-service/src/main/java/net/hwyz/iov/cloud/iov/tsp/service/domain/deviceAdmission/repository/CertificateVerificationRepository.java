package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository;

/**
 * 证书验证仓储接口
 *
 * @author hwyz_leo
 */
public interface CertificateVerificationRepository {

    /**
     * 检查证书是否被吊销
     * @param hsm 设备HSM标识
     * @return true-已吊销, false-未吊销
     */
    boolean checkRevocation(String hsm);
}