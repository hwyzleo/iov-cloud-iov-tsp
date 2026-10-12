package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DEVICE_ONLINE 消费者支撑 Bean 配置
 *
 * @author hwyz_leo
 */
@Configuration
public class DeviceOnlineConsumerSupportConfig {

    @Bean
    public DeviceOnlineConsumerState deviceOnlineConsumerState() {
        return new DeviceOnlineConsumerState();
    }
}
