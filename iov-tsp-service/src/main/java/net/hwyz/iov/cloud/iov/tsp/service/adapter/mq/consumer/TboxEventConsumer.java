package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.TboxCmdAppService;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspLegacyTboxEventProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.stereotype.Component;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverOptions;

import jakarta.annotation.PostConstruct;
import java.util.Collections;

/**
 * 车联终端事件消息消费者（旧 vagw-tbox-event 链路）
 * <p>
 * TSP-DSN-CR-010：DEVICE_ONLINE 已迁移至独立 VagwDeviceOnlineConsumer（vagw.device.online），
 * 本消费者不再处理 DEVICE_ONLINE，也不再引用旧 TboxDeviceOnlineEvent。
 * 旧 Producer 代码仍可能存在，但当前环境不存在 vagw-tbox-event 物理 Topic，旧链路无有效生产消费能力。
 * <p>
 * CMD_ACK/FIND_VEHICLE 当前不可用，由后续 TSP-CR-011 / EDD-VAGW-CR-013 恢复新远控通道；
 * 本消费者默认由 {@code tsp.kafka.legacy-tbox-event.enabled=false} 关闭，不对不存在的旧 Topic 发起 metadata 请求。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TboxEventConsumer {

    private final KafkaProperties properties;
    private final TspLegacyTboxEventProperties legacyProperties;

    @Value("${spring.kafka.consumer.reactive-concurrency:5}")
    private Integer concurrency;

    private final TboxCmdAppService tboxAppService;

    /**
     * 消费车辆接入网关事件消息（仅旧远控分支，默认关闭）
     */
    @PostConstruct
    public void consume() {
        if (!legacyProperties.isEnabled()) {
            log.warn("旧 TBOX 事件消费者已禁用（CMD_ACK/FIND_VEHICLE 待 TSP-CR-011 恢复）, topic={}",
                    legacyProperties.getTopic());
            return;
        }
        ReceiverOptions<byte[], byte[]> options = ReceiverOptions.create(properties.buildConsumerProperties());
        options = options.subscription(Collections.singleton(legacyProperties.getTopic()));
        log.info("开始监听车辆接入网关事件消息（旧链路）");
        new ReactiveKafkaConsumerTemplate<>(options)
                .receiveAutoAck()
                .flatMap(record -> {
                    String vin = null;
                    try {
                        vin = new String(record.key());
                        String eventJson = new String(record.value());
                        if (StrUtil.isNotBlank(vin)) {
                            log.debug("收到车辆[{}]事件消息[{}]", vin, eventJson);
                            JSONObject event = JSONUtil.parseObj(eventJson);
                            String eventType = event.getStr("type");
                            switch (eventType) {
                                case "CMD_ACK" ->
                                        tboxAppService.cmdAck(event.getStr("cmdId"), event.getDate("ackTime"));
                                case "FIND_VEHICLE" -> tboxAppService.remoteControlResponse(vin, eventType, event);
                                case "DEVICE_ONLINE" ->
                                        log.warn("旧链路 DEVICE_ONLINE 已迁移至 vagw.device.online，忽略事件[{}]", eventJson);
                                default -> log.warn("收到未知类型事件消息[{}]", eventJson);
                            }
                        } else {
                            log.warn("收到缺失VIN的异常指令消息[{}]", eventJson);
                        }
                    } catch (Exception e) {
                        log.error("消费车辆[{}]事件消息异常", vin, e);
                    }
                    return Mono.empty();
                }, concurrency)
                .doOnError(throwable -> log.error("消费车辆事件消息异常", throwable))
                .subscribe();
    }
}
