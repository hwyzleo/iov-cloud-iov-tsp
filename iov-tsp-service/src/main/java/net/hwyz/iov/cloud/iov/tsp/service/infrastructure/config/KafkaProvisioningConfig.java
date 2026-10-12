package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TSP Kafka Admin 配置
 * <p>
 * 启用 FW-KAFKA Topic Provisioning 时，框架 {@code KafkaTopicProvisioningAutoConfiguration} 定义了
 * 名为 {@code kafkaAdmin} 的 {@link Admin} Bean，与 Spring Boot {@code KafkaAutoConfiguration} 的
 * {@code kafkaAdmin}（{@code KafkaAdmin} 类型）同名冲突（覆盖被禁用会导致启动失败）。
 * <p>
 * 此处显式提供一个不同名的 {@code Admin} Bean：框架侧 {@code @ConditionalOnMissingBean(Admin.class)}
 * 因已存在 {@link Admin} 而退让，Spring Boot 的 {@code kafkaAdmin} 正常注册，冲突消除。
 * Admin 连接参数复用 {@link KafkaProperties}（Nacos kafka.yaml 提供 bootstrap-servers）。
 *
 * @author hwyz_leo
 */
@Configuration
public class KafkaProvisioningConfig {

    /**
     * Kafka Admin 客户端，应用关闭时释放资源。
     */
    @Bean(destroyMethod = "close")
    public Admin kafkaAdminClient(KafkaProperties properties) {
        return AdminClient.create(properties.buildAdminProperties());
    }
}
