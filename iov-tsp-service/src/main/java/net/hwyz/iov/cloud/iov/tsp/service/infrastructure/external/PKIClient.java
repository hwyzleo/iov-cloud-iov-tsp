package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.external;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.PKIServiceUnavailableException;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.CertificateVerificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * PKI服务客户端
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class PKIClient implements CertificateVerificationRepository {

    private final RestTemplate restTemplate;
    private final String pkiServiceUrl;

    public PKIClient(
            @Value("${pki.service.url:http://pki-service}") String pkiServiceUrl,
            @Value("${pki.service.timeout:5000}") long timeoutMs) {
        this.restTemplate = new RestTemplate();
        this.pkiServiceUrl = pkiServiceUrl;
    }

    /**
     * 检查证书是否被吊销
     * @param hsm 设备HSM标识
     * @return true-已吊销, false-未吊销
     */
    @Override
    public boolean checkRevocation(String hsm) {
        try {
            // 调用PKI服务检查证书吊销状态
            String url = pkiServiceUrl + "/api/pki/v1/revocation/check?hsm=" + hsm;
            Boolean response = restTemplate.getForObject(url, Boolean.class);
            return Boolean.TRUE.equals(response);
        } catch (Exception e) {
            log.warn("PKI服务调用失败: {}", e.getMessage());
            throw new PKIServiceUnavailableException("PKI服务调用失败");
        }
    }
}
