package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.TboxCmdAppService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverOptions;

import javax.annotation.PostConstruct;
import java.util.Collections;

/**
 * 车联终端事件消息消费者
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RefreshScope
@RequiredArgsConstructor
public class TboxEventConsumer {

    private final KafkaProperties properties;

    @Value("${spring.kafka.consumer.reactive-concurrency:5}")
    private Integer concurrency;

    private final String TOPIC_VAGW_TBOX_EVENT = "vagw-tbox-event";

    private final TboxCmdAppService tboxAppService;

    /**
     * 消费车辆接入网关事件消息
     */
    @PostConstruct
    public void consume() {
        ReceiverOptions<byte[], byte[]> options = ReceiverOptions.create(properties.buildConsumerProperties());
        options = options.subscription(Collections.singleton(TOPIC_VAGW_TBOX_EVENT));
        log.info("开始监听车辆接入网关事件消息");
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
                .doOnError(throwable -> {
                    log.error("消费车辆事件消息异常", throwable);
                })
                .subscribe();
    }

}
