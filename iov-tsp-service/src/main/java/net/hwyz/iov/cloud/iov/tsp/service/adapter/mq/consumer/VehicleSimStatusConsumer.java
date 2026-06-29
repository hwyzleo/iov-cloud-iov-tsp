package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.event.CcsSimStatusChangedEvent;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.VehicleNetworkAppService;
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
 * 车辆SIM状态事件消费者
 * 订阅 ccs-sim-status-changed topic
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RefreshScope
@RequiredArgsConstructor
public class VehicleSimStatusConsumer {

    private final KafkaProperties properties;

    @Value("${spring.kafka.consumer.reactive-concurrency:5}")
    private Integer concurrency;

    private final String TOPIC_CCS_SIM_STATUS_CHANGED = "ccs-sim-status-changed";

    private final VehicleNetworkAppService vehicleNetworkAppService;
    private final SimStatusEventTranslator simStatusEventTranslator;

    /**
     * 消费CCS SIM状态变更事件
     */
    @PostConstruct
    public void consume() {
        ReceiverOptions<byte[], byte[]> options = ReceiverOptions.create(properties.buildConsumerProperties());
        options = options.subscription(Collections.singleton(TOPIC_CCS_SIM_STATUS_CHANGED));
        log.info("开始监听CCS SIM状态变更事件消息");
        new ReactiveKafkaConsumerTemplate<>(options)
                .receiveAutoAck()
                .flatMap(record -> {
                    String vin = null;
                    try {
                        vin = new String(record.key());
                        String eventJson = new String(record.value());
                        if (StrUtil.isNotBlank(vin)) {
                            log.debug("收到CCS SIM状态变更事件: vin={}", vin);
                            JSONObject event = JSONUtil.parseObj(eventJson);

                            CcsSimStatusChangedEvent ccsEvent = CcsSimStatusChangedEvent.builder()
                                    .vin(vin)
                                    .iccid(event.getStr("iccid"))
                                    .simStatus(event.getInt("simStatus"))
                                    .realnameStatus(event.getInt("realnameStatus"))
                                    .version(event.getLong("version"))
                                    .build();

                            // 防腐层翻译
                            CcsSimStatusChangedEvent translatedEvent = simStatusEventTranslator.translate(ccsEvent);

                            // 调用应用服务处理
                            vehicleNetworkAppService.handleSimStatusChanged(translatedEvent);

                            log.info("CCS SIM状态变更事件处理成功: vin={}, iccid={}, version={}",
                                    vin, ccsEvent.getIccid(), ccsEvent.getVersion());
                        } else {
                            log.warn("收到缺失VIN的CCS SIM状态变更事件[{}]", eventJson);
                        }
                    } catch (Exception e) {
                        log.error("消费CCS SIM状态变更事件消息异常: vin={}", vin, e);
                    }
                    return Mono.empty();
                }, concurrency)
                .doOnError(throwable -> {
                    log.error("消费CCS SIM状态变更事件消息异常", throwable);
                })
                .subscribe();
    }
}
