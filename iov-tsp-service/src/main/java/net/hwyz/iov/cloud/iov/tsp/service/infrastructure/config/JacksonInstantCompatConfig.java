package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson Instant 兼容配置（TSP-DSN-CR-012 加固）。
 * <p>
 * 为 Spring Boot 全局 ObjectMapper 注册 {@link JacksonInstantCompatModule}：
 * {@code Instant} 字段同时接受 ISO-8601 字符串与 epoch 毫秒数字，覆盖
 * JavaTimeModule "数字按 epoch 秒"的默认行为。
 * <p>
 * 必须使用 {@code modulesToInstall} 而非 {@code modules}：{@code modules(...)}
 * 会关闭 {@code Jackson2ObjectMapperBuilder} 的 well-known modules 自动发现
 * （JavaTimeModule 不再注册），导致响应序列化 {@code Instant} 时抛
 * {@code InvalidDefinitionException: Java 8 date/time type Instant not supported}；
 * {@code modulesToInstall} 仅追加模块，JavaTimeModule 仍由自动发现注册（序列化器），
 * 本模块注册在其后（反序列化器覆盖），两类能力并存。
 * <p>
 * 影响范围：所有注入 Spring ObjectMapper 的消费者（V2C 聚合成功事件、设备上线事件等）
 * 与 Web 层序列化；对 ISO 字符串生产者完全向后兼容，仅修正数字语义。
 *
 * @author hwyz_leo
 * @since 2026-10-27
 */
@Configuration
public class JacksonInstantCompatConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer instantEpochMilliCustomizer() {
        return builder -> builder.modulesToInstall(new JacksonInstantCompatModule());
    }
}
