package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.VehicleBindingProjectionAppService;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.VehicleNetworkAppService;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehiclePartBindingChangedEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverOptions;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.Collections;

@Slf4j
@Component
@RefreshScope
@RequiredArgsConstructor
public class VehiclePartBindingChangedConsumer {

    private final KafkaProperties properties;

    @Value("${spring.kafka.consumer.reactive-concurrency:5}")
    private Integer concurrency;

    private final String TOPIC_VMD_VEHICLE_BINDING_CHANGED = "vmd-vehicle-binding-changed";

    private final VehicleBindingProjectionAppService projectionAppService;
    private final VehicleNetworkAppService vehicleNetworkAppService;

    @PostConstruct
    public void consume() {
        ReceiverOptions<String, String> options = ReceiverOptions.<String, String>create(properties.buildConsumerProperties())
                .withKeyDeserializer(new StringDeserializer())
                .withValueDeserializer(new StringDeserializer());
        options = options.subscription(Collections.singleton(TOPIC_VMD_VEHICLE_BINDING_CHANGED));
        log.info("开始监听VMD车辆绑定变更事件消息");
        new ReactiveKafkaConsumerTemplate<>(options)
                .receiveAutoAck()
                .flatMap(record -> {
                    String vin = null;
                    try {
                        vin = record.key();
                        String eventJson = record.value();
                        if (vin != null && !vin.isEmpty()) {
                            log.debug("收到车辆[{}]绑定变更事件[{}]", vin, eventJson);
                            JSONObject event = JSONUtil.parseObj(eventJson);

                            VehiclePartBindingChangedEvent bindingEvent = VehiclePartBindingChangedEvent.builder()
                                .vin(vin)
                                .bindingId(event.getLong("bindingId"))
                                .partCode(event.getStr("partCode"))
                                .sn(event.getStr("sn"))
                                .deviceCategory(event.getStr("deviceCategory"))
                                .vehicleNodeCode(event.getStr("vehicleNodeCode"))
                                .iccid1(event.getStr("iccid1"))
                                .iccid2(event.getStr("iccid2"))
                                .changeType(event.getStr("changeType"))
                                .replaceOfBindingId(event.getLong("replaceOfBindingId"))
                                .occurredAt(Instant.parse(event.getStr("occurredAt")))
                                .seq(event.getLong("seq"))
                                .build();

                            projectionAppService.handleBindingChangedEvent(bindingEvent);
                            
                            if (StrUtil.isNotBlank(bindingEvent.getIccid1())) {
                                vehicleNetworkAppService.handleBindingChanged(vin, 1, bindingEvent.getIccid1());
                            }
                            if (StrUtil.isNotBlank(bindingEvent.getIccid2())) {
                                vehicleNetworkAppService.handleBindingChanged(vin, 2, bindingEvent.getIccid2());
                            }
                        } else {
                            log.warn("收到缺失VIN的异常绑定变更消息[{}]", eventJson);
                        }
                    } catch (Exception e) {
                        log.error("消费车辆[{}]绑定变更事件消息异常", vin, e);
                    }
                    return Mono.empty();
                }, concurrency)
                .doOnError(throwable -> {
                    log.error("消费车辆绑定变更事件消息异常", throwable);
                })
                .subscribe();
    }
}
