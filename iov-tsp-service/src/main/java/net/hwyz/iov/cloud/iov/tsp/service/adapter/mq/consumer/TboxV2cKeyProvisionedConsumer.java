package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.event.TboxV2cKeyProvisionedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.TboxRegistrationAppService;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverOptions;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * TBOX V2C 业务密钥件内导入完成事件消费者
 * <p>
 * 订阅 vmd.tbox-v2c-key-provisioned（VMD CR-058 聚合成功事件，Kafka Key=deviceSn），
 * 校验 Kafka Key 与 deviceSn 一致后交由 TboxRegistrationAppService 幂等收口设备注册激活。
 * <p>
 * 消费能力受发布门禁 tsp.tbox-registration.v2c-activation.enabled 控制，默认关闭。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RefreshScope
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tsp.tbox-registration.v2c-activation", name = "enabled", havingValue = "true")
public class TboxV2cKeyProvisionedConsumer {

    private final KafkaProperties properties;

    @Value("${spring.kafka.consumer.reactive-concurrency:5}")
    private Integer concurrency;

    private final String TOPIC_VMD_TBOX_V2C_KEY_PROVISIONED = "vmd.tbox-v2c-key-provisioned";

    private final TboxRegistrationAppService tboxRegistrationAppService;

    /**
     * 消费 VMD TBOX V2C 业务密钥件内导入完成事件
     */
    @PostConstruct
    public void consume() {
        ReceiverOptions<String, String> options = ReceiverOptions.<String, String>create(properties.buildConsumerProperties())
                .withKeyDeserializer(new StringDeserializer())
                .withValueDeserializer(new StringDeserializer());
        options = options.subscription(Collections.singleton(TOPIC_VMD_TBOX_V2C_KEY_PROVISIONED));
        log.info("开始监听VMD TBOX V2C业务密钥导入完成事件消息");
        new ReactiveKafkaConsumerTemplate<>(options)
                .receiveAutoAck()
                .flatMap(record -> {
                    String deviceSnKey = null;
                    try {
                        deviceSnKey = record.key();
                        String eventJson = record.value();
                        if (StrUtil.isBlank(eventJson)) {
                            log.warn("收到空的V2C密钥导入完成事件");
                            return Mono.empty();
                        }
                        log.debug("收到V2C密钥导入完成事件: key={}, payload={}", deviceSnKey, eventJson);
                        JSONObject event = JSONUtil.parseObj(eventJson);

                        TboxV2cKeyProvisionedEvent provisionedEvent = TboxV2cKeyProvisionedEvent.builder()
                                .eventId(event.getStr("eventId"))
                                .requestId(event.getStr("requestId"))
                                .deviceSn(event.getStr("deviceSn"))
                                .businessDomain(event.getStr("businessDomain"))
                                .keys(parseKeys(event.getJSONArray("keys")))
                                .result(event.getStr("result"))
                                .occurredAt(parseInstant(event.getStr("occurredAt")))
                                .build();

                        if (StrUtil.isBlank(deviceSnKey) || !deviceSnKey.equals(provisionedEvent.getDeviceSn())) {
                            log.warn("V2C密钥导入完成事件 Kafka Key 与 deviceSn 不一致: key={}, deviceSn={}，拒绝处理",
                                    deviceSnKey, provisionedEvent.getDeviceSn());
                            return Mono.empty();
                        }

                        tboxRegistrationAppService.handleV2cProvisioned(provisionedEvent);
                    } catch (Exception e) {
                        log.error("消费V2C密钥导入完成事件消息异常: key={}", deviceSnKey, e);
                    }
                    return Mono.empty();
                }, concurrency)
                .doOnError(throwable -> {
                    log.error("消费V2C密钥导入完成事件消息异常", throwable);
                })
                .subscribe();
    }

    private List<TboxV2cKeyProvisionedEvent.Key> parseKeys(JSONArray array) {
        List<TboxV2cKeyProvisionedEvent.Key> keys = new ArrayList<>();
        if (array != null) {
            for (Object item : array) {
                if (item instanceof JSONObject keyObject) {
                    keys.add(TboxV2cKeyProvisionedEvent.Key.builder()
                            .purpose(keyObject.getStr("purpose"))
                            .keyId(keyObject.getStr("keyId"))
                            .businessKeyVersion(keyObject.getLong("businessKeyVersion"))
                            .build());
                }
            }
        }
        return keys;
    }

    private Instant parseInstant(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            try {
                Date date = DateUtil.parse(value);
                return date.toInstant();
            } catch (Exception ex) {
                log.warn("无法解析V2C事件时间[{}]，按当前时间处理", value);
                return null;
            }
        }
    }

}
