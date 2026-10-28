package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;
import java.time.Instant;

/**
 * 兼容 ISO-8601 与 epoch 毫秒的 {@link Instant} 反序列化器（TSP-DSN-CR-012 加固）。
 * <p>
 * wire 契约（与 VMD 生产者对齐）：
 * <ul>
 *   <li>字符串：按 ISO-8601 解析（如 {@code 2026-10-27T14:57:34.340Z}），格式非法时抛
 *       {@link java.time.format.DateTimeParseException}（fail-closed）；</li>
 *   <li>数字：一律视为 <b>epoch 毫秒</b>（hutool {@code JSONUtil.toJsonStr} 对 {@code Instant}
 *       的默认输出），兼容旧版 VMD 生产者（v1 契约）与 DLT/源 Topic 中已存在的旧消息。</li>
 * </ul>
 * <p>
 * 背景：Jackson 默认将数字按 <b>epoch 秒</b> 解析 {@code Instant}，导致 epoch 毫秒（如
 * {@code 1793113054340}）被误读为公元 58791 年，进而触发消费者 {@code OCCURRED_AT_FUTURE}
 * 永久拒绝；本反序列化器将数字语义修正为 epoch 毫秒。
 *
 * @author hwyz_leo
 * @since 2026-10-27
 */
public class InstantEpochMilliDeserializer extends JsonDeserializer<Instant> {

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.currentToken();
        if (token == JsonToken.VALUE_STRING) {
            String text = p.getText().trim();
            if (text.isEmpty()) {
                return null;
            }
            return Instant.parse(text);
        }
        if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
            // 数字 = epoch 毫秒；小数部分视为毫秒内亚精度，直接截断
            return Instant.ofEpochMilli(p.getLongValue());
        }
        throw JsonMappingException.from(p,
                "Instant 仅支持 ISO-8601 字符串或 epoch 毫秒数字");
    }
}
