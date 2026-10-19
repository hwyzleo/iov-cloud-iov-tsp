package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * V2C 聚合成功事件消费者支撑 Bean 配置（TSP-DSN-CR-012）
 *
 * @author hwyz_leo
 */
@Configuration
public class TboxV2cKeyProvisionedConsumerSupportConfig {

    @Bean
    public TboxV2cKeyProvisionedConsumerState tboxV2cKeyProvisionedConsumerState() {
        return new TboxV2cKeyProvisionedConsumerState();
    }
}
