package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import com.fasterxml.jackson.databind.module.SimpleModule;

import java.time.Instant;

/**
 * Instant 兼容模块：注册 {@link InstantEpochMilliDeserializer}，覆盖 JSR310 默认
 * "数字按 epoch 秒"的语义。注册顺序在 JavaTimeModule 之后，后者先注册的先失效。
 *
 * @author hwyz_leo
 * @since 2026-10-27
 */
public class JacksonInstantCompatModule extends SimpleModule {

    public JacksonInstantCompatModule() {
        super("iov-instant-compat");
        addDeserializer(Instant.class, new InstantEpochMilliDeserializer());
    }
}
