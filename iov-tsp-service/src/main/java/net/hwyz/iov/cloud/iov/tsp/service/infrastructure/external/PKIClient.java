package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.external;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.PKIServiceUnavailableException;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.repository.CertificateVerificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.Duration;

/**
 * PKI服务客户端
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class PKIClient implements CertificateVerificationRepository {

    private final WebClient webClient;
    private final Duration timeout;

    public PKIClient(
            @Value("${pki.service.url:http://pki-service}") String pkiServiceUrl,
            @Value("${pki.service.timeout:5000}") long timeoutMs) {
        this.webClient = WebClient.builder()
            .baseUrl(pkiServiceUrl)
            .build();
        this.timeout = Duration.ofMillis(timeoutMs);
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
            // 示例实现，需要根据实际PKI服务接口调整
            Boolean response = webClient.get()
                .uri("/api/pki/v1/revocation/check?hsm={hsm}", hsm)
                .retrieve()
                .bodyToMono(Boolean.class)
                .timeout(timeout)
                .block();

            return Boolean.TRUE.equals(response);
        } catch (WebClientRequestException e) {
            log.warn("PKI服务请求异常: {}", e.getMessage());
            throw new PKIServiceUnavailableException("PKI服务不可用");
        } catch (Exception e) {
            log.warn("PKI服务调用失败: {}", e.getMessage());
            throw new PKIServiceUnavailableException("PKI服务调用失败");
        }
    }
}
